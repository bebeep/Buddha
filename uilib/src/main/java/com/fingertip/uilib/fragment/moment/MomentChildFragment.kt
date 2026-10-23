package com.fingertip.uilib.fragment.moment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.view.SampleCoverVideo
import com.fingertip.uilib.R
import com.fingertip.uilib.activity.MomentVideoFeedActivity
import com.fingertip.uilib.adapter.MomentAdapter
import com.fingertip.uilib.databinding.FragmentMomentChildBinding
import com.fingertip.uilib.fragment.MainFragment
import com.fingertip.uilib.viewmodel.MomentVM
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView
import org.greenrobot.eventbus.Subscribe

class MomentChildFragment : TopPmFragment<MomentVM>() {

    companion object {
        const val FOLLOW = "FOLLOW" //关注
        const val MOMENT = "MOMENT" //佛友圈
        const val TYPE_STRING = "typeString"

        /** 视频至少露出这么多才自动播放 */
        private const val MIN_VISIBLE_RATIO = 0.5f

        /** 当前没在播时放宽一点，露出一部分就先放起来，省得用户以为坏了 */
        private const val MIN_VISIBLE_RATIO_IDLE = 0.2f

        fun newInstance(typeString: String = MOMENT): MomentChildFragment {
            return MomentChildFragment().apply {
                arguments = Bundle().apply {
                    putString(TYPE_STRING, typeString)
                }
            }
        }
    }

    override fun layoutId(): Int = R.layout.fragment_moment_child

    override fun initVM() = MomentVM()

    private val binding get() = mBinding as FragmentMomentChildBinding

    /** 当前正在播放视频的位置，-1 表示无 */
    private var currentPlayingPosition = -1

    /** 已标记为已读的动态ID集合，避免重复请求 */
    private val viewedMomentIds = mutableSetOf<Int>()

    var adapter: MomentAdapter? = null
    val list = ArrayList<MomentEntity>()
    var pageCount = 0
    var momentType = MOMENT

    override fun onNewBundle(args: Bundle?) {
        super.onNewBundle(args)
        initShiTu()
    }

    override fun initShiTu() {
        momentType = arguments?.getString(TYPE_STRING, MOMENT) ?: MOMENT
        initAdapter()
        mViewModel.getMomentList(pageCount, if (momentType == FOLLOW) 1 else 0)
    }

    private fun initAdapter() {
        adapter = MomentAdapter(
            requireContext(),
            onItemClick = { entity ->
                (parentFragment?.parentFragment as MainFragment)
                    .start(MomentDetailsFragment.newInstance(entity.momentId, entity))
            },
            onVideoClick = { entity -> openVideoFeed(entity) }
        )
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = adapter
        adapter?.initData(list)

        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    //滑动过程中不打断当前视频，停稳了再切到该播的那条，避免一路暂停/恢复
                    binding.recyclerview.post { playBestVisible() }
                    markVisibleMomentsAsViewed()
                }
            }
        })

        binding.srl.setOnRefreshLoadMoreListener(object : com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                pageCount = 0
                mViewModel.getMomentList(pageCount, if (momentType == FOLLOW) 1 else 0)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                pageCount++
                mViewModel.getMomentList(pageCount, if (momentType == FOLLOW) 1 else 0)
            }
        })
    }

    /**
     * 进全屏短视频页：只带「有视频的动态」，定位到当前这条
     */
    private fun openVideoFeed(entity: MomentEntity) {
        val videos = list.filter { it.momentType == 2 && !it.videoUrl.isNullOrEmpty() }
        if (videos.isEmpty()) return
        val index = videos.indexOfFirst { it.momentId == entity.momentId }.coerceAtLeast(0)
        //进去之前先把自己的声音停掉，两边同时出声就乱了
        pauseCurrentVideo()
        MomentVideoFeedActivity.start(requireContext(), videos, index)
    }

    /**
     * 将当前可见区域的动态标记为已读，批量调用接口，已请求过的不重复请求
     */
    private fun markVisibleMomentsAsViewed() {
        val layoutManager = binding.recyclerview.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION) return

        val newIds = mutableListOf<Int>()
        for (i in firstVisible..lastVisible) {
            val item = adapter?.get(i) ?: continue
            if (item.momentId !in viewedMomentIds) {
                newIds.add(item.momentId)
                viewedMomentIds.add(item.momentId)
            }
        }
        if (newIds.isNotEmpty()) {
            mViewModel.viewMoment(newIds)
        }
    }

    /**
     * 从可见范围里挑露出面积最大的一条视频来播，都没有就停掉
     */
    private fun playBestVisible() {
        if (!isResumed) return
        val layoutManager = binding.recyclerview.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION) return

        var target = -1
        var bestRatio = 0f
        for (i in firstVisible..lastVisible) {
            val item = adapter?.get(i) ?: continue
            if (item.momentType != 2 || item.videoUrl.isNullOrEmpty()) continue
            val holder = binding.recyclerview.findViewHolderForAdapterPosition(i) as? MomentAdapter.MomentVH
                ?: continue
            val ratio = visibleRatio(holder.itemView)
            if (ratio > bestRatio) {
                bestRatio = ratio
                target = i
            }
        }

        if (target == -1) {
            if (currentPlayingPosition != -1) pauseCurrentVideo()
            return
        }

        //已经在播的那条，只要还露着大半就别切，避免滑动时来回抢
        val threshold = if (currentPlayingPosition == -1) MIN_VISIBLE_RATIO_IDLE else MIN_VISIBLE_RATIO
        if (bestRatio < threshold) {
            if (currentPlayingPosition != -1) pauseCurrentVideo()
            return
        }
        if (target != currentPlayingPosition) playVideo(target)
    }

    /**
     * item 露在 RecyclerView 里的比例
     */
    private fun visibleRatio(view: View): Float {
        val height = view.height
        if (height <= 0) return 0f
        val recyclerHeight = binding.recyclerview.height
        val top = view.top
        val visible = minOf(top + height, recyclerHeight) - maxOf(top, 0)
        return visible.toFloat() / height
    }

    /**
     * 播第 position 条视频。同一条已经在播/暂停时直接续，不重新 prepare
     */
    private fun playVideo(position: Int) {
        val entity = adapter?.get(position) ?: return
        val url = entity.videoUrl ?: return
        val holder = binding.recyclerview.findViewHolderForAdapterPosition(position)
            as? MomentAdapter.MomentVH ?: return
        val video = holder.binding.video
        log(value = "playVideo-----------$url")

        if (video.tag == url) {
            when (video.currentState) {
                GSYVideoView.CURRENT_STATE_PLAYING,
                GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START,
                GSYVideoView.CURRENT_STATE_PREPAREING -> {
                    currentPlayingPosition = position
                    return
                }

                GSYVideoView.CURRENT_STATE_PAUSE -> {
                    video.onVideoResume()
                    currentPlayingPosition = position
                    return
                }
            }
        }

        //换视频前先把上一个释放掉，两个 surface 抢同一个播放核心会黑一下
        val old = GSYVideoManager.instance().listener()
        if (old is SampleCoverVideo && old !== video) old.release()

        video.tag = url
        video.setUp(url, true, null)
        video.setIsTouchWiget(false)
        video.isLooping = true
        video.setVideoAllCallBack(object : GSYSampleCallBack() {
            override fun onAutoComplete(url: String?, vararg objects: Any?) {
                super.onAutoComplete(url, *objects)
                currentPlayingPosition = -1
            }

            override fun onPlayError(url: String?, vararg objects: Any?) {
                super.onPlayError(url, *objects)
                currentPlayingPosition = -1
            }
        })
        video.startPlayLogic()
        currentPlayingPosition = position
    }

    /**
     * 暂停当前播放的视频
     */
    private fun pauseCurrentVideo() {
        val player = GSYVideoManager.instance().listener()
        if (player is SampleCoverVideo && player.currentState == GSYVideoView.CURRENT_STATE_PLAYING) {
            player.onVideoPause()
        }
        currentPlayingPosition = -1
    }

    /**
     * 释放视频资源，防止内存泄漏
     */
    private fun releaseVideo() {
        val player = GSYVideoManager.instance().listener()
        if (player is SampleCoverVideo) {
            player.release()
        }
        currentPlayingPosition = -1
    }

    override fun initObserver() {
        super.initObserver()
        mViewModel.momentListResult.observe(viewLifecycleOwner) {
            log(value = "momentList-----------$it")
            binding.srl.finishRefresh()
            binding.srl.finishLoadMore()
            if (it.success) {
                if (pageCount == 0) list.clear()
                it.data?.let { data ->
                    list.addAll(data)
                    //数据换了，之前记的位置不再可信
                    currentPlayingPosition = -1
                    adapter?.initData(list) ?: log(value = "adapter is null")
                    binding.recyclerview.post {
                        playBestVisible()
                        // 首次加载完成后，标记初始可见动态为已读
                        markVisibleMomentsAsViewed()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        releaseVideo()
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        pauseCurrentVideo()
    }

    override fun onResume() {
        super.onResume()
        //回来时播放器可能已经被全屏页释放掉了，重新挑一条来播
        val player = GSYVideoManager.instance().listener()
        if (player is SampleCoverVideo && player.currentState == GSYVideoView.CURRENT_STATE_PAUSE) {
            player.onVideoResume()
        } else {
            currentPlayingPosition = -1
            binding.recyclerview.post { playBestVisible() }
        }
    }

    @Subscribe()
    fun onEvent(event: MessageEvent) {
        when (event.what) {
            EventConstant.EVENT_NOTIFY_MOMENT_LIST -> {
                val momentEntity = event.`object` as? MomentEntity ?: return
                val position = list.indexOfFirst { it.momentId == momentEntity.momentId }
                if (position != -1) {
                    list[position] = momentEntity
                    //只刷这一条，别整表刷新，否则正在播的视频会被重置
                    adapter?.notifyItemChanged(position)
                }
            }
        }
    }
}

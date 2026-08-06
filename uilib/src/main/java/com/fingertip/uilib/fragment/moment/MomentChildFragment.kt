package com.fingertip.uilib.fragment.moment

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.view.SampleCoverVideo
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MomentAdapter
import com.fingertip.uilib.databinding.FragmentMomentChildBinding
import com.fingertip.uilib.fragment.MainFragment
import com.fingertip.uilib.viewmodel.MomentVM
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView

class MomentChildFragment : TopPmFragment<MomentVM>() {
    override fun layoutId(): Int = R.layout.fragment_moment_child

    override fun initVM() = MomentVM()
    private val binding get() = mBinding as FragmentMomentChildBinding

    /** 当前正在播放视频的位置，-1 表示无 */
    private var currentPlayingPosition = -1

    /** 已标记为已读的动态ID集合，避免重复请求 */
    private val viewedMomentIds = mutableSetOf<Int>()

    companion object {
        const val FOLLOW = "FOLLOW" //关注
        const val MOMENT = "MOMENT" //佛友圈
        const val TYPE_STRING = "typeString"
        fun newInstance(typeString: String = MOMENT): MomentChildFragment {
            return MomentChildFragment().apply {
                arguments = Bundle().apply {
                    putString(TYPE_STRING, typeString)
                }
            }
        }
    }

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
        adapter = MomentAdapter(requireContext()) {
            log(value = "MomentAdapter-----------")
            (parentFragment?.parentFragment as MainFragment).start(MomentDetailsFragment.newInstance(it.momentId,it))
        }
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = adapter
        adapter?.initData(list)

        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        //滑动停止，自动播放可见区域的第一个视频
                        autoPlayVideo()
                        //滑动停止后，将可见动态标记为已读
                        markVisibleMomentsAsViewed()
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        //拖动时暂停，避免滑动时视频声音干扰
                        pauseCurrentVideo()
                    }
                }
            }
        })

        binding.srl.setOnRefreshLoadMoreListener(object : com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                pageCount = 0
                mViewModel.getMomentList(pageCount, if (momentType == FOLLOW) 1 else 0)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                pageCount ++
                mViewModel.getMomentList(pageCount, if (momentType == FOLLOW) 1 else 0)
            }
        })
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
     * 播放指定位置的视频
     */
    private fun playVideo(position: Int) {
        val entity = adapter?.get(position) ?: return
        val url = entity.videoUrl ?: return
        log(value = "playVideo-----------$url")

        //先释放旧的播放器
        val oldPlayer = GSYVideoManager.instance().listener()
        if (oldPlayer is SampleCoverVideo) {
            oldPlayer.release()
        }

        val holder = binding.recyclerview.findViewHolderForAdapterPosition(position) ?: return
        val videoView = holder.itemView.findViewById<SampleCoverVideo>(R.id.video) ?: return

        //设置新播放器
        videoView.setUp(url, true, null)
        videoView.setIsTouchWiget(false)
        videoView.isLooping = true
        videoView.setVideoAllCallBack(object : GSYSampleCallBack() {
            override fun onPrepared(url: String?, vararg objects: Any?) {
                super.onPrepared(url, *objects)
                log(value = "playVideo onPrepared: $url")
            }

            override fun onAutoComplete(url: String?, vararg objects: Any?) {
                super.onAutoComplete(url, *objects)
                currentPlayingPosition = -1
                log(value = "playVideo onAutoComplete: $url")
            }

            override fun onPlayError(url: String?, vararg objects: Any?) {
                super.onPlayError(url, *objects)
                log(value = "playVideo onPlayError: $url")
            }

            override fun onQuitFullscreen(url: String?, vararg objects: Any?) {
                super.onQuitFullscreen(url, *objects)
                log(value = "playVideo onQuitFullscreen: $url")
            }
        })
        videoView.startPlayLogic()
        currentPlayingPosition = position
    }

    /**
     * 自动播放当前可见区域的第一个视频
     */
    private fun autoPlayVideo() {
        val layoutManager = binding.recyclerview.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION) return

        for (i in firstVisible..lastVisible) {
            val item = adapter?.get(i) ?: continue
            if (item.momentType == 2 && !item.videoUrl.isNullOrEmpty()) {
                if (currentPlayingPosition != i) {
                    playVideo(i)
                }
                break
            }
        }
    }

    /**
     * 暂停当前播放的视频
     */
    private fun pauseCurrentVideo() {
        val player = GSYVideoManager.instance().listener()
        if (player is SampleCoverVideo && player.currentState == GSYVideoView.CURRENT_STATE_PLAYING) {
            player.onVideoPause()
        }
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
                it.data?.let { it1 ->
                    list.addAll(it1)
                    adapter?.initData(list) ?: log(value = "adapter is null")
                    binding.recyclerview.post {
                        autoPlayVideo()
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
        //恢复时如果之前有播放，继续播放
        val player = GSYVideoManager.instance().listener()
        if (player is SampleCoverVideo && player.currentState == GSYVideoView.CURRENT_STATE_PAUSE) {
            player.onVideoResume()
        }
    }
}

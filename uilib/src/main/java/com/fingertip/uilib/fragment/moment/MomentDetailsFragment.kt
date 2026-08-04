package com.fingertip.uilib.fragment.moment

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.KeyboardUtils
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.TimeUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.baselib.view.SampleCoverVideo
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CommentAdapter
import com.fingertip.uilib.databinding.FragmentMomentDetailsBinding
import com.fingertip.uilib.viewmodel.MomentVM
import com.lzlz.toplib.extention.gone
import com.lzlz.toplib.extention.invisible
import com.lzlz.toplib.extention.visible
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView

class MomentDetailsFragment: TopPmFragment<MomentVM>() {
    override fun initVM() = MomentVM ()

    override fun layoutId() = R.layout.fragment_moment_details

    private val binding get() = mBinding as FragmentMomentDetailsBinding


    companion object {

        const val MOMENT_ID = "MOMENT_ID"
        const val MOMENT_DATA = "MOMENT_DATA"
        fun newInstance(momentId : Int, entity : MomentEntity? = null) : MomentDetailsFragment {
            return MomentDetailsFragment().apply {
                arguments = Bundle().apply {
                    putInt(MOMENT_ID, momentId)
                    if (entity != null) {
                        putSerializable(MOMENT_DATA, entity)
                    }
                }
            }
        }
    }

    var momentId = 0
    var momentEntity: MomentEntity? = null

    lateinit var commentAdapter: CommentAdapter

    override fun getClickViews(): List<View> {
        return listOf(binding.ivBack,binding.ivHead,binding.tvNickname,binding.ivMenu,binding.tvFollow,
            binding.tvShare,binding.tvLike,binding.tvInput,binding.ivSend,binding.flInput)
    }

    override fun onNewBundle(args: Bundle?) {
        super.onNewBundle(args)
        initShiTu()
    }

    override fun initShiTu() {
        momentId = arguments?.getInt(MOMENT_ID) ?: 0
        momentEntity = arguments?.getSerializable(MOMENT_DATA) as? MomentEntity

        log(value = "momentId:$momentId , momentEntity:$momentEntity")
        if (momentId == 0)
        {
            pop()
            return
        }

        initAdapter()
        if (momentEntity == null)
        {
            mViewModel.getMomentDetails(momentId)
        }
        else
        {
            refreshUI()
        }
    }


    override fun initObserver() {
        super.initObserver()
        mViewModel.momentDetailsResult.observe(this){
            if (it.success)
            {
                momentEntity = it.data
                refreshUI()
            }
        }
        mViewModel.momentCommentListResult.observe(this)
        {
            if (it.success)
            {
                commentAdapter.initData(it.data)
            }
        }

        mViewModel.momentCommentDetailsResult.observe(this)
        {
            if (it.success)
            {

            }
        }
    }


    private fun initAdapter(){
        commentAdapter = CommentAdapter(requireContext()){ position, innerPosition, longClickPosition, viewId->

        }

        binding.rvComment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComment.adapter = commentAdapter

        mViewModel.getCommentList(momentId,1)
    }


    private fun refreshUI(){
        momentEntity?.let {
            binding.ivHead.loadHead(it.postAvatar)
            binding.tvNickname.text = it.postUserName
            binding.tvContent.text = it.textContent?:""
            binding.tvLevel.text = "lv.${it.userLevel}·${TimeUtil.dateFormateTime(it.createDate)}"
            binding.tvFollow.isSelected = it.isFollowed
            binding.tvFollow.text = if (it.isFollowed) "已关注" else "+关注"
            binding.tvContent.visibility = if (it.textContent.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.flImageVideo.visibility = if (it.momentType==1) View.GONE else View.VISIBLE
            binding.cvVideo.visibility = if (it.momentType==2) View.VISIBLE else View.GONE
            binding.rvPhotos.visibility = if (it.momentType==3 && it.imageUrl.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvLocate.visibility = if (it.postAddress?.isNotEmpty() == true) View.VISIBLE else View.GONE
            binding.tvLocate.text = it.postAddress
            binding.tvShare.text = "${it.shareCount}"
            binding.tvLike.text = "${it.likeCount}"
            binding.tvComments.text = "评论(${it.commentCount})"


            if (it.momentType == 2 && !it.videoUrl.isNullOrEmpty() && it.videoWidth > 0 && it.videoHeight > 0) {
                val screenWidth = binding.cvVideo.resources.displayMetrics.widthPixels - (binding.cvVideo.resources.displayMetrics.density * 16 * 2).toInt()
                if (it.videoHeight > it.videoWidth)
                {
                    binding.cvVideo.layoutParams = (binding.cvVideo.layoutParams as ViewGroup.LayoutParams).apply {
                        width = (screenWidth * 0.6).toInt()
                        height = (screenWidth * 0.6 * (it.videoHeight.toFloat()/it.videoWidth.toFloat())).toInt()
                    }
                }
                else
                {
                    binding.cvVideo.layoutParams = (binding.cvVideo.layoutParams as ViewGroup.LayoutParams).apply {
                        width = screenWidth
                        height = (screenWidth * it.videoHeight.toFloat() / it.videoWidth.toFloat()).toInt()
                    }
                }

                binding.video.loadCoverImage(it.videoCover, com.fingertip.baselib.R.mipmap.icon_default_img)
                initVideoPlayer()
            }

        }

    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v?.id){
            R.id.iv_back -> pop()
            R.id.iv_head,R.id.tv_nickname -> {

            }
            R.id.iv_menu -> {

            }
            R.id.tv_follow -> {

            }
            R.id.tv_share -> {

            }
            R.id.tv_like -> {

            }
            R.id.tv_input -> {
                binding.flInput.invisible()
                binding.llInput.visible()
                binding.etComment.requestFocus()
                KeyboardUtils.showSoftInput(binding.etComment)
            }
            R.id.iv_send -> {
                binding.flInput.visible()
                binding.llInput.invisible()
                binding.etComment.clearFocus()
                KeyboardUtils.hideSoftInput(binding.etComment)
            }
        }
    }


    /**
     * 初始化播放器
     */
    private fun initVideoPlayer() {
        val url = momentEntity?.videoUrl ?: return
        log(value = "playVideo-----------$url")

        //先释放旧的播放器
        val oldPlayer = GSYVideoManager.instance().listener()
        if (oldPlayer is SampleCoverVideo) {
            oldPlayer.release()
        }

        //设置新播放器
        binding.video.setUp(url, true, null)
        binding.video.isLooping = true
        binding.video.setVideoAllCallBack(object : GSYSampleCallBack() {
            override fun onPrepared(url: String?, vararg objects: Any?) {
                super.onPrepared(url, *objects)
                log(value = "playVideo onPrepared: $url")
            }

            override fun onAutoComplete(url: String?, vararg objects: Any?) {
                super.onAutoComplete(url, *objects)
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
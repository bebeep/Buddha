package com.fingertip.uilib.fragment.moment

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.KeyboardUtils
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.event_bus.EventBusProxy
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.TimeUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.baselib.view.SampleCoverVideo
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CommentAdapter
import com.fingertip.uilib.adapter.MomentImageAdapter
import com.fingertip.uilib.dialog.CommentDetailDialog
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

        // descendantFocusability 常量
        private const val FOCUS_BLOCK_DESCENDANTS = 0x00020000
        private const val FOCUS_BEFORE_DESCENDANTS = 0x00040000

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
    lateinit var momentImageAdapter: MomentImageAdapter

    private var keyboardListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var isKeyboardShowing = false

    private var parentCommentId: Int = 0
    private var targetCommentId: Int = 0
    private var commentDetailDialog: CommentDetailDialog? = null

    private var commentPageCount = 0

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
        setupKeyboardListener()
        setupDismissKeyboardOnTouch()
        // 进入详情时标记为已读
        mViewModel.viewMoment(listOf(momentId))
        if (momentEntity == null)
        {
            mViewModel.getMomentDetails(momentId)
        }
        else
        {
            refreshUI()
        }

        binding.srl.setOnLoadMoreListener {
            commentPageCount++
            mViewModel.getCommentList(momentId,commentPageCount)
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
                if (commentPageCount == 0)
                {
                    commentAdapter.initData(it.data)
                }
                else
                {
                    commentAdapter.addData(it.data)
                }
                if (it.data == null || it.data?.isEmpty() == true)
                {
                    binding.srl.finishLoadMoreWithNoMoreData()
                }
                else
                {
                    binding.srl.finishLoadMore()
                }
            }
        }


        mViewModel.commentResult.observe(this)
        {
            loadEnding()
            if (it.success)
            {
                if (parentCommentId == 0) //添加主评论
                {
                    commentAdapter.addData(it.data,0)
                }
                else //刷新内部评论
                {
                    val entityIndex = commentAdapter.mlist.indexOfFirst { entity-> entity.id == parentCommentId }
                    if (entityIndex!=-1)
                    {
                        if (commentAdapter.mlist[entityIndex].childComment == null)
                        {
                            commentAdapter.mlist[entityIndex].childComment = ArrayList()
                        }
                        commentAdapter.mlist[entityIndex].childComment?.add(0,it.data!!)
                        commentAdapter.notifyItemChanged(entityIndex)
                    }
                }
                momentEntity!!.commentCount+=1
                binding.tvComments.text = "评论(${momentEntity!!.commentCount})"
                notifyMomentListUpdate()
            }
            parentCommentId = 0
            targetCommentId = 0

        }
    }


    private fun initAdapter(){
        commentAdapter = CommentAdapter(requireContext()){ position, innerPosition, longClickPosition, viewId->
            if (longClickPosition!=-1)
            {
                return@CommentAdapter
            }
            when(viewId){
                R.id.tv_like -> {
                    //点赞
                    val parentComment = commentAdapter.get(position) ?: return@CommentAdapter
                    mViewModel.likeMomentComment(parentComment.id,!parentComment.isLiked)
                    return@CommentAdapter
                }
                R.id.tv_inner_like -> {
                    //点赞
                    val childComment = commentAdapter.get(position)?.childComment?.get(innerPosition) ?: return@CommentAdapter
                    mViewModel.likeMomentComment(childComment.id,!childComment.isLiked)
                    return@CommentAdapter
                }
                R.id.iv_head,R.id.tv_nickname->{//主评论头像昵称-跳转资料页

                }
                R.id.iv_inner_head,R.id.tv_inner_nickname->{//子评论头像昵称-跳转资料页

                }
                R.id.tv_more->{//查看评论详情
                    val parentComment = commentAdapter.get(position) ?: return@CommentAdapter
                    log(tag="parentComment","details: ${parentComment==null}")
                    if (commentDetailDialog == null)
                    {
                        commentDetailDialog = CommentDetailDialog(requireContext())
                        commentDetailDialog?.onLikeClick = { parentCommentPosition,childCommentPosition,childComment->
                            if (parentCommentPosition!=-1)
                            {
                                commentAdapter.notifyInnerComment(parentCommentPosition,childCommentPosition,childComment)
                            }
                        }
                    }
                    commentDetailDialog?.show(position,parentComment)
                }
                R.id.cl_parent,R.id.cl_inner_parent -> {//回复评论
                    //回复
                    val parentComment = commentAdapter.get(position) ?: return@CommentAdapter
                    parentCommentId = parentComment.id
                    if (innerPosition!=-1 && parentComment.childComment!=null && parentComment.childComment!!.size>innerPosition)
                    {
                        targetCommentId = parentComment.childComment!![innerPosition].id
                    }
                    // 允许子View优先获取焦点，EditText需要
                    binding.clMomentParent.descendantFocusability = FOCUS_BEFORE_DESCENDANTS
                    binding.flInput.gone()
                    binding.llInput.visible()
                    binding.etComment.requestFocus()
                    KeyboardUtils.showSoftInput(binding.etComment)
                    binding.etComment.hint = "回复 ${parentComment.senderNickName}"
                }
            }
        }

        binding.rvComment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComment.adapter = commentAdapter

        mViewModel.getCommentList(momentId,commentPageCount)


        momentImageAdapter = MomentImageAdapter(requireContext(),0,0){

        }
        binding.rvPhotos.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvPhotos.adapter = momentImageAdapter
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
            binding.tvLike.isSelected = it.isLiked


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
            else if (it.momentType == 3 && it.imageUrl.isNotEmpty()) {
                momentImageAdapter.initData(it.imageUrl)
            }

        }

    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v?.id){
            R.id.iv_back -> pop()
            R.id.iv_head,R.id.tv_nickname -> {//跳转资料页

            }
            R.id.iv_menu -> {//举报

            }
            R.id.tv_follow -> {//关注

            }
            R.id.tv_share -> {//分享

            }
            R.id.tv_like -> {//点赞
                mViewModel.likeMoment(momentId,!momentEntity?.isLiked!!)
                { isLike,success->
                    if ( success)
                    {
                        momentEntity?.isLiked = isLike

                        if (isLike) momentEntity!!.likeCount +=1
                        else momentEntity!!.likeCount -=1

                        binding.tvLike.text = "${0.coerceAtLeast(momentEntity!!.likeCount)}"
                        binding.tvLike.isSelected = isLike
                        notifyMomentListUpdate()
                    }
                }
            }
            R.id.tv_input -> {
                // 允许子View优先获取焦点，EditText需要
                binding.clMomentParent.descendantFocusability = FOCUS_BEFORE_DESCENDANTS
                binding.flInput.gone()
                binding.llInput.visible()
                binding.etComment.requestFocus()
                KeyboardUtils.showSoftInput(binding.etComment)
            }
            R.id.iv_send -> {
                // 恢复阻止子View获取焦点
                binding.clMomentParent.descendantFocusability = FOCUS_BLOCK_DESCENDANTS
                KeyboardUtils.hideSoftInput(binding.etComment)
                binding.llInput.gone()
                binding.flInput.visible()
                startWaiting()
                mViewModel.commentMoment(momentId,parentCommentId,targetCommentId,binding.etComment.text.toString())
                binding.etComment.setText("")
                binding.etComment.hint = "加入讨论吧"
                binding.etComment.clearFocus()
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


    /**
     * 触摸空白区域时隐藏软键盘
     * 利用 FullTopConstraintLayout 的 onPreDispatchTouchListener，
     * 在子View处理触摸之前检测并隐藏键盘
     */
    private fun setupDismissKeyboardOnTouch() {
        binding.clMomentParent.onPreDispatchTouchListener = { event ->
            val b = mBinding as? FragmentMomentDetailsBinding
            if (b != null && isKeyboardShowing) {
                val x = event.x
                val y = event.y
                // 检查触摸点是否在 ll_input 区域外
                if (x < b.llInput.left || x > b.llInput.right ||
                    y < b.llInput.top || y > b.llInput.bottom) {
                    b.clMomentParent.descendantFocusability = FOCUS_BLOCK_DESCENDANTS
                    b.etComment.clearFocus()
                    KeyboardUtils.hideSoftInput(b.etComment)
                    b.llInput.gone()
                    b.flInput.visible()
                }
            }
            false // 不消费事件，继续分发给子View
        }
    }

    /**
     * 监听键盘高度变化，手动调整 ll_input 位置
     * 因为 SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 导致 adjustResize 失效
     */
    private fun setupKeyboardListener() {
        keyboardListener = ViewTreeObserver.OnGlobalLayoutListener {
            // View销毁后回调仍可能触发，需做空安全检查
            val b = mBinding as? FragmentMomentDetailsBinding ?: return@OnGlobalLayoutListener
            val rect = Rect()
            b.clMomentParent.getWindowVisibleDisplayFrame(rect)
            val screenHeight = b.clMomentParent.rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            if (keyboardHeight > screenHeight / 4) {
                // 键盘显示，调整 ll_input 的底部边距
                isKeyboardShowing = true
                val lp = b.llInput.layoutParams as ViewGroup.MarginLayoutParams
                if (lp.bottomMargin != keyboardHeight) {
                    lp.bottomMargin = keyboardHeight
                    b.llInput.layoutParams = lp
                }
            } else if (isKeyboardShowing) {
                // 键盘隐藏，恢复底部边距
                isKeyboardShowing = false
                val lp = b.llInput.layoutParams as ViewGroup.MarginLayoutParams
                if (lp.bottomMargin != 0) {
                    lp.bottomMargin = 0
                    b.llInput.layoutParams = lp
                }
            }
        }
        binding.clMomentParent.viewTreeObserver.addOnGlobalLayoutListener(keyboardListener)
    }

    private fun removeKeyboardListener() {
        val b = mBinding as? FragmentMomentDetailsBinding ?: run {
            keyboardListener = null
            return
        }
        keyboardListener?.let {
            b.clMomentParent.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        keyboardListener = null
    }

    override fun onDestroyView() {
        removeKeyboardListener()
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

    fun notifyMomentListUpdate() {
        EventBusProxy.post(MessageEvent(EventConstant.EVENT_NOTIFY_MOMENT_LIST,momentEntity))
    }
}
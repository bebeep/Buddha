package com.fingertip.uilib.dialog

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.KeyboardUtils
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.baselib.util.TimeUtil
import com.fingertip.baselib.util.clearFirstLine
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CommentInnerAdapter
import com.fingertip.uilib.databinding.DialogCommentDetailBinding
import com.fingertip.uilib.viewmodel.MomentVM
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lzlz.toplib.extention.setColoredText

/**
 * 评论详情弹窗
 * 展示主评论及子评论，子评论支持分页加载
 */
class CommentDetailDialog(context: Context) : TopDialogBottomSheetDialog(context) {


    override fun getLayoutId() = R.layout.dialog_comment_detail

    private var parentComment: CommentEntity? = null

    private val vm = MomentVM()
    private val childComments = mutableListOf<CommentEntity>()
    private lateinit var binding: DialogCommentDetailBinding
    private lateinit var adapter: CommentInnerAdapter


    private var currentPage = -1
    private var noMore = false
    private var isAlive = true

    var parentCommentPosition = -1

    var onLikeClick:(position: Int,innerPosition:Int, comment: CommentEntity)->Unit = {_,_,_->}

    override fun onViewCreate(view: View) {
        binding = DialogCommentDetailBinding.bind(view)

        // 弹出时直接满屏
        behavior.skipCollapsed = true
        behavior.isDraggable = false
        behavior.state = BottomSheetBehavior.STATE_EXPANDED


        adapter = CommentInnerAdapter(context) { position, longClickPosition, viewId ->
            when (viewId) {
                R.id.iv_inner_head, R.id.tv_inner_nickname -> {
                    // 跳转资料页（暂空）
                }
                R.id.tv_inner_like -> {
                    //点赞
                    val childComment = adapter.get(position)?: return@CommentInnerAdapter
                    vm.likeMomentComment(childComment.id,!childComment.isLiked)
                    onLikeClick(parentCommentPosition,position,childComment)
                    return@CommentInnerAdapter
                }
            }
        }

        binding.rvComment.layoutManager = LinearLayoutManager(context)
        binding.rvComment.adapter = adapter
        // 禁用嵌套滚动，避免SmartRefreshLayout拦截fling事件导致下滑无惯性
        binding.rvComment.isNestedScrollingEnabled = false



        // 上拉加载更多
        binding.srl.setOnLoadMoreListener {
            loadMore()
        }



    }

    /**
     * 加载更多子评论
     */
    private fun loadMore() {
        if (noMore) {
            binding.srl.finishLoadMoreWithNoMoreData()
            return
        }
        currentPage++
        vm.getCommentDetails(parentComment!!.id, currentPage)
    }



    override fun onStart() {
        super.onStart()
        // 设置弹窗高度固定为屏幕高度的70%
        val dm = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(dm)
        val targetHeight = (dm.heightPixels * 0.7).toInt()
        behavior.peekHeight = targetHeight
        window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            it.layoutParams.height = targetHeight
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface?) {
        isAlive = false
        super.onDismiss(dialog)
    }

    fun show(parentCommentPosition:Int,parentComment: CommentEntity?)
    {
        if (parentComment == null) return
        this.parentCommentPosition = parentCommentPosition
        this.parentComment = parentComment

        log(tag="parentComment","${binding==null} ${parentComment==null}")
        // 初始化主评论头部
        binding.ivHead.loadHead(parentComment.senderAvatar)
        binding.tvNickname.text = parentComment.senderNickName
        binding.tvLevel.text =
            "lv.${parentComment.senderLevel}·${TimeUtil.dateFormateTime(parentComment.commentDate)}"
        binding.tvComment.text = parentComment.textContent?.clearFirstLine() ?: ""
        if (!parentComment.replyNickName.isNullOrEmpty()) {
            binding.tvComment.setColoredText(
                "回复 ${parentComment.replyNickName}: ${parentComment.textContent?.clearFirstLine() ?: ""}",
                parentComment.replyNickName!!, "#33000000"
            )
        }

        vm.momentCommentDetailsResult.observeForever { result ->
            if (!isAlive) return@observeForever
            if (result.success) {
                val newData = result.data ?: emptyList()
                if (newData.isEmpty()) {
                    noMore = true
                    binding.srl.finishLoadMoreWithNoMoreData()
                } else {
                    if (currentPage == 0)
                    {
                        childComments.clear()
                        adapter.initData(newData)
                    }
                    else
                        adapter.addData(newData)
                    childComments.addAll(newData)
                    binding.srl.finishLoadMore()
                    // 如果累计数量已达到总数，不再加载更多
                    if (childComments.size >= parentComment.childCommentCount) {
                        noMore = true
                    }
                }
            } else {
                binding.srl.finishLoadMore(false)
            }
            show()
            if (currentPage == 0){
                binding.rvComment.scrollToPosition(0)
            }
        }

        currentPage = 0
        noMore = false
        isAlive = true
        vm.getCommentDetails(parentComment.id, currentPage)
    }
}

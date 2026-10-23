package com.fingertip.uilib.dialog

import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.KeyboardUtils
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CommentAdapter
import com.fingertip.uilib.databinding.DialogMomentCommentBinding
import com.fingertip.uilib.viewmodel.MomentVM

/**
 * 短视频全屏页的评论面板（从底部升起，高度约 7 成屏）。
 *
 * 和抖音一致的行为：
 * - 面板常驻输入框，点某条评论切成「回复 xxx」；
 * - 上拉分页加载，二级回复超过 3 条走 [CommentDetailDialog] 看全部；
 * - 发评论成功后本地插入并回调 [onCommentCountChanged]，外层据此刷新右下角评论数。
 *
 * 键盘适配没依赖 adjustResize（全屏沉浸式下不可靠），而是自己量键盘高度，
 * 把面板底部抬起来并同步缩减面板高度，保证输入框始终贴在键盘上沿。
 */
class MomentCommentDialog(
    context: Context,
    private val momentId: Int,
    initialCount: Int = 0
) : TopDialog(context) {

    companion object {
        /** 面板初始高度占屏幕的比例 */
        private const val PANEL_HEIGHT_RATIO = 0.72f

        /** 键盘顶起来以后，面板最小保留高度（占屏幕比例），别把列表挤没 */
        private const val MIN_PANEL_HEIGHT_RATIO = 0.34f

        private const val DEFAULT_HINT = "善语结善缘，说点什么吧"
    }

    override fun attachLayoutRes() = R.layout.dialog_moment_comment

    override fun IS_MATCH_PARENT() = true

    private val vm = MomentVM()
    private lateinit var binding: DialogMomentCommentBinding
    private lateinit var adapter: CommentAdapter

    private var detailDialog: CommentDetailDialog? = null
    private var commentCount = initialCount
    private var pageCount = 0
    private var noMore = false
    private var sending = false
    private var isAlive = true

    /** 回复的主评论 / 目标评论，0 表示发主评论 */
    private var parentCommentId = 0
    private var targetCommentId = 0

    /** 面板高度相关的像素值 */
    private var panelBaseHeight = 0
    private var minPanelHeight = 0
    private var keyboardListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    /** 评论数变化，外层用来同步右下角评论数 */
    var onCommentCountChanged: (count: Int) -> Unit = {}

    private val commentListObserver = Observer<RequestResult<List<CommentEntity>>> { result ->
        if (!isAlive) return@Observer
        if (result.success) {
            val data = result.data ?: emptyList()
            if (pageCount == 0) adapter.initData(data) else adapter.addData(data)
            if (data.isEmpty()) {
                noMore = true
                binding.srl.finishLoadMoreWithNoMoreData()
            } else {
                binding.srl.finishLoadMore()
            }
        } else {
            binding.srl.finishLoadMore(false)
            if (pageCount > 0) pageCount--
        }
        refreshEmptyState()
    }

    private val commentObserver = Observer<RequestResult<CommentEntity>> { result ->
        if (!isAlive) return@Observer
        sending = false
        log(tag = "momentComment", value = "result=${result.success} code=${result.errorCode} msg=${result.errorMsg} id=${result.data?.id}")
        if (result.success) {
            val entity = result.data
            if (entity != null) {
                insertComment(entity)
                commentCount = (commentCount + 1).coerceAtLeast(1)
                refreshTitle()
                onCommentCountChanged(commentCount)
            }
            resetInput()
        } else {
            ToastUtil.showMessage(result.errorMsg ?: "评论失败，请稍后再试")
        }
        parentCommentId = 0
        targetCommentId = 0
    }

    override fun onViewInit(view: View?) {
        binding = DialogMomentCommentBinding.bind(view!!)
        setBottomDialog()
        //优先让系统 resize（多数机型 dialog 上是生效的）；不生效的机型交给下面的手动键盘监听兜底
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window?.setDimAmount(0.5f)

        initPanelSize()
        initList()
        initInput()

        vm.momentCommentListResult.observeForever(commentListObserver)
        vm.commentResult.observeForever(commentObserver)

        setupKeyboardListener()

        refreshTitle()
        refreshEmptyState()
        vm.getCommentList(momentId, pageCount)
    }

    private fun initPanelSize() {
        val dm = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(dm)
        panelBaseHeight = (dm.heightPixels * PANEL_HEIGHT_RATIO).toInt()
        minPanelHeight = (dm.heightPixels * MIN_PANEL_HEIGHT_RATIO).toInt()
        val lp = binding.llPanel.layoutParams as ViewGroup.MarginLayoutParams
        lp.height = panelBaseHeight
        binding.llPanel.layoutParams = lp
    }

    private fun initList() {
        adapter = CommentAdapter(context) { position, innerPosition, longClickPosition, viewId ->
            //长按暂不处理
            if (longClickPosition != -1) return@CommentAdapter
            val comment = adapter.get(position) ?: return@CommentAdapter
            when (viewId) {
                R.id.tv_like -> {
                    //CommentAdapter 自己会翻转 UI 和计数，这里只需要同步数据并调接口
                    val like = !comment.isLiked
                    comment.isLiked = like
                    vm.likeMomentComment(comment.id, like)
                }

                R.id.tv_inner_like -> {
                    //内部评论的点赞态由 CommentInnerAdapter 自己维护，
                    //它是在回调之后才翻转 isLiked，所以这里读到的还是旧值
                    val child = comment.childComment?.getOrNull(innerPosition) ?: return@CommentAdapter
                    vm.likeMomentComment(child.id, !child.isLiked)
                }

                R.id.tv_more -> {
                    //子评论还没全部拉下来，复用详情弹窗分页看
                    if (detailDialog == null) {
                        detailDialog = CommentDetailDialog(context).apply {
                            //详情弹窗里点了赞，回来整体刷一下列表（数据不多，直接重建）
                            onLikeClick = { _, _, _ -> adapter.notifyDataSetChanged() }
                        }
                    }
                    detailDialog?.show(position, comment)
                }

                R.id.cl_parent, R.id.cl_inner_parent -> replyTo(comment, innerPosition)
            }
        }

        binding.rvComment.layoutManager = LinearLayoutManager(context)
        binding.rvComment.adapter = adapter

        binding.srl.setOnLoadMoreListener {
            if (noMore) {
                binding.srl.finishLoadMoreWithNoMoreData()
                return@setOnLoadMoreListener
            }
            pageCount++
            vm.getCommentList(momentId, pageCount)
        }

        binding.ivClose.setOnClickListener { dismiss() }
        binding.vOutside.setOnClickListener { dismiss() }
    }

    private fun initInput() {
        binding.ivSend.setOnClickListener { sendComment() }
        binding.etComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendComment()
                true
            } else {
                false
            }
        }
    }

    private fun replyTo(comment: CommentEntity, innerPosition: Int) {
        parentCommentId = comment.id
        val child = if (innerPosition >= 0) comment.childComment?.getOrNull(innerPosition) else null
        targetCommentId = child?.id ?: 0
        val nickName = child?.senderNickName ?: comment.senderNickName
        binding.etComment.hint = "回复 ${nickName.orEmpty()}"
        binding.etComment.requestFocus()
        KeyboardUtils.showSoftInput(binding.etComment)
    }

    private fun sendComment() {
        if (sending) return
        val text = binding.etComment.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            ToastUtil.showMessage("说点什么吧")
            return
        }
        sending = true
        log(tag = "momentComment", value = "send momentId=$momentId parent=$parentCommentId target=$targetCommentId text=$text")
        vm.commentMoment(momentId, parentCommentId, targetCommentId, text)
    }

    /**
     * 新评论插到列表里：主评论插顶部，回复插到对应主评论的子评论最前面
     */
    private fun insertComment(entity: CommentEntity) {
        if (parentCommentId == 0) {
            adapter.addData(entity, 0)
            binding.rvComment.scrollToPosition(0)
            return
        }
        val index = adapter.mlist.indexOfFirst { it.id == parentCommentId }
        if (index < 0) {
            //主评论不在当前页，退化成插到列表顶部
            adapter.addData(entity, 0)
            binding.rvComment.scrollToPosition(0)
            return
        }
        val parent = adapter.mlist[index]
        if (parent.childComment == null) parent.childComment = ArrayList()
        parent.childComment?.add(0, entity)
        parent.childCommentCount += 1
        adapter.notifyItemChanged(index)
    }

    private fun resetInput() {
        binding.etComment.setText("")
        binding.etComment.hint = DEFAULT_HINT
        binding.etComment.clearFocus()
        KeyboardUtils.hideSoftInput(binding.etComment)
    }

    private fun refreshTitle() {
        binding.tvTitle.text = if (commentCount > 0) "全部评论($commentCount)" else "全部评论"
    }

    private fun refreshEmptyState() {
        val empty = adapter.mlist.isEmpty()
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.srl.visibility = if (empty) View.GONE else View.VISIBLE
    }

    //------------------------- 键盘 -------------------------

    private fun setupKeyboardListener() {
        keyboardListener = ViewTreeObserver.OnGlobalLayoutListener { syncPanelSize() }
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(keyboardListener)
    }

    /**
     * 面板高度跟着键盘走。
     *
     * 注意坐标体系：getWindowVisibleDisplayFrame 给的是屏幕坐标，root 高度是窗口内高度，
     * 两者直接相减会漏掉状态栏那一段（实测少 90 多 px，表现为输入框被键盘压住一半），
     * 所以先把窗口底边换算成屏幕坐标再算键盘高度。
     */
    private fun syncPanelSize() {
        if (!isAlive) return
        val root = binding.root
        val rootHeight = root.height
        if (rootHeight <= 0) return

        val base = (rootHeight * PANEL_HEIGHT_RATIO).toInt()
        minPanelHeight = (rootHeight * MIN_PANEL_HEIGHT_RATIO).toInt()

        val rect = Rect()
        root.getWindowVisibleDisplayFrame(rect)
        val location = IntArray(2)
        root.getLocationOnScreen(location)
        val rootBottomOnScreen = location[1] + rootHeight
        val keyboardHeight = (rootBottomOnScreen - rect.bottom).coerceAtLeast(0)

        val showing = keyboardHeight > rootHeight / 4
        if (showing) {
            applyPanelHeight(base - keyboardHeight, keyboardHeight)
        } else {
            applyPanelHeight(base, 0)
        }
    }

    /**
     * 面板贴着屏幕底部，键盘弹起时把底边抬到键盘上沿，同时把高度减掉同样的值，
     * 这样面板顶部位置不动，看起来就像键盘把面板「压扁」了
     */
    private fun applyPanelHeight(height: Int, bottomMargin: Int) {
        val lp = binding.llPanel.layoutParams as ViewGroup.MarginLayoutParams
        val targetHeight = height.coerceAtLeast(minPanelHeight)
        if (lp.height == targetHeight && lp.bottomMargin == bottomMargin) return
        lp.height = targetHeight
        lp.bottomMargin = bottomMargin
        binding.llPanel.layoutParams = lp
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        isAlive = false
        vm.momentCommentListResult.removeObserver(commentListObserver)
        vm.commentResult.removeObserver(commentObserver)
        keyboardListener?.let {
            if (binding.root.viewTreeObserver.isAlive) {
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(it)
            }
        }
        keyboardListener = null
        if (detailDialog?.isShowing == true) detailDialog?.dismiss()
        detailDialog = null
        super.onDismiss(dialog)
    }
}

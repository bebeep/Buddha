package com.fingertip.uilib.fragment.book

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.fingertip.baselib.bean.BookComment
import com.fingertip.baselib.bean.BuddhaBook
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.dialog.RemindAllDialog
import com.fingertip.baselib.log
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FojingCommentAdapter
import com.fingertip.uilib.adapter.FojingRecommendAdapter
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ColorUtil
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.databinding.FragFojingDetailsBinding
import com.google.android.material.appbar.AppBarLayout
import com.lzlz.toplib.extention.gone
import com.lzlz.toplib.extention.isVisible
import com.lzlz.toplib.extention.toPx
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener
import java.io.File
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.min

/**
 * 佛经-书籍详情
 */
class FojingDetailsFragment :TopVMFragment<BookshelfVM>(),OnLoadMoreListener{
    private val binding get() = mBinding as FragFojingDetailsBinding
    override fun layoutId() = R.layout.frag_fojing_details
    override fun initVM() = BookshelfVM()

    companion object {
        const val BOOK_ID = "BOOK_ID"
        private const val ACTION_SHARE_CHOSEN = "com.fingertip.uilib.ACTION_SHARE_CHOSEN"
        fun newInstance(bookId: Int): FojingDetailsFragment {
            return FojingDetailsFragment().apply {
                arguments = Bundle().apply {
                    putInt(BOOK_ID, bookId)
                }
            }
        }
    }

    lateinit var recommendAdapter: FojingRecommendAdapter
    lateinit var commentAdapter: FojingCommentAdapter

    var bookId: Int = 0
    var currPage : Int = 0
    var bookInfo: BuddhaBook ? = null

    private var keyboardListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    /**
     * 注册监听的 ViewTreeObserver。
     * 保存该实例用于反注册，避免视图销毁后 mBinding 为 null 时取不到 ViewTreeObserver。
     */
    private var keyboardObserver: ViewTreeObserver? = null

    /**
     * 键盘隐藏时可视区域底部的屏幕坐标，用于计算真实键盘高度（消除状态栏/导航栏误差）
     */
    private var lastVisibleBottom = 0

    /**
     * 用户在分享面板中选中目标应用后的回调（并非分享真正完成，系统分享没有完成回调）
     */
    private val shareChosenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            val component = intent.getParcelableExtra<ComponentName>(Intent.EXTRA_CHOSEN_COMPONENT)
            val appName = component?.className?.substringAfterLast('.') ?: ""
            //TODO 如需上报服务端分享次数，可在此处调用 mViewModel 的上报接口
            ToastUtil.showMessage("已分享至${appName}")
            log("TAG","已分享至${appName}")
        }
    }

    @SuppressLint("WrongConstant")
    override fun initShiTu() {
        bookId = arguments?.getInt(BOOK_ID, 0) ?: 0
        if (bookId <= 0) {
            return
        }
        //注册分享目标选中回调
        ContextCompat.registerReceiver(
            requireContext(), shareChosenReceiver,
            IntentFilter(ACTION_SHARE_CHOSEN), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        initAdapter()

        binding.appbar.addOnOffsetChangedListener { appBarLayout, scrollY ->
            val slideOffset = min(abs(scrollY * 1.0f / 100.toPx()), 1f)
            binding.clTitle.setBackgroundColor(
                ColorUtil.changeAlpha(
                    resources.getColor(R.color.white),
                    slideOffset
                )
            )
            binding.tvTitle.alpha = slideOffset
        }
        binding.srl.setOnLoadMoreListener(this)

        binding.srl.setOnRefreshListener {
            currPage = 0
            mViewModel.getBookInfoByBookId(bookId)
            mViewModel.getBookComment(bookId,currPage)
        }

        //根据 et_comment 是否输入内容控制 iv_send 是否可点击
        binding.etComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.ivSend.isEnabled = !s.isNullOrBlank()
            }
        })
        binding.ivSend.isEnabled = !binding.etComment.text.isNullOrBlank()

        setupKeyboardListener()

        mViewModel.getBookInfoByBookId(bookId)
        mViewModel.getBookComment(bookId,currPage)
    }

    override fun getClickViews(): List<View> {
        return listOf(binding.vBack.ivBack, binding.ivShare, binding.tvRead, binding.tvCopy, binding.btnAddShelf, binding.btnAddComment, binding.ivSend)
    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v?.id){
            R.id.v_back -> pop()
            R.id.iv_share -> shareBook()
            R.id.tv_read -> {
                start(FojingReadFragment())
            }
            R.id.tv_copy -> {
                ToastUtil.showMessage("抄经模式")
                start(FojingCopyFragment())
            }
            R.id.btn_add_shelf -> switchShelf()
            R.id.btn_add_comment -> showCommentInput()
            R.id.iv_send -> sendComment()
        }
    }


    override fun initObserver() {
        super.initObserver()
        mViewModel.bookInfoResult.observe(this){
            if (it.success)
            {
                bookInfo = it.data
                refreshUI()
            }
        }

        mViewModel.bookCommentResult.observe(this)
        {
            binding.srl.finishLoadMore()
            if (it.success)
            {
                commentAdapter.addData(it.data)
            }
        }
    }

    override fun onLoadMore(refreshLayout: RefreshLayout) {
        currPage ++
        mViewModel.getBookComment(bookId,currPage)
    }


    private fun initAdapter(){
        recommendAdapter = FojingRecommendAdapter(requireContext(),{ viewId, data ->
            when(viewId){
                //点赞
                R.id.ll_like -> {
                    mViewModel.likeBookComment(bookId, data.id, !data.isLiked, { success->
                        if (success)
                        {
                            data.isLiked = !data.isLiked
                            data.likeCount = if (data.isLiked) data.likeCount + 1 else data.likeCount - 1
                            recommendAdapter.notifyItemChanged(recommendAdapter.mlist.indexOf(data), FojingRecommendAdapter.PAYLOAD_LIKE)
                        }
                    })
                }
            }
        },{ viewId, entity ->
            when(viewId){
                //长按评论内容
                R.id.tv_master_content -> showDeleteCommentDialog(entity)
            }
        })
        binding.rvRecommend.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecommend.adapter = recommendAdapter

        commentAdapter = FojingCommentAdapter(requireContext(), onItemClick = { viewId, data ->
            when(viewId){
                //点赞
                R.id.ll_like -> {
                    mViewModel.likeBookComment(bookId, data.id, !data.isLiked, { success->
                        if (success)
                        {
                            data.isLiked = !data.isLiked
                            data.likeCount = if (data.isLiked) data.likeCount + 1 else data.likeCount - 1
                            commentAdapter.notifyItemChanged(commentAdapter.mlist.indexOf(data), FojingCommentAdapter.PAYLOAD_LIKE)
                        }
                    })
                }
            }
        }, onItemLongClick = { viewId, data ->
            when(viewId){
                //长按评论内容
                R.id.tv_comment_content -> showDeleteCommentDialog(data)
            }
        })
        binding.rvComment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComment.adapter = commentAdapter

    }

    /**
     * 长按评论内容：仅自己发布的评论可删除，二次确认后调接口删除并刷新列表
     */
    private fun showDeleteCommentDialog(comment: BookComment) {
        //判断该评论是否由自己发布
        val myAccountId = GlobalConfig.userData?.accountId ?: 0
        if (myAccountId <= 0 || comment.accountId != myAccountId) {
            return
        }
        RemindAllDialog(
            requireActivity(),
            endAction = {
                mViewModel.removeBookComment(comment.id) { success ->
                    if (!isAdded) return@removeBookComment
                    if (!success) {
                        ToastUtil.showMessage("删除失败，请重试")
                        return@removeBookComment
                    }
                    if (comment.isMasterRecommend)
                    {
                        val index = recommendAdapter.mlist.indexOf(comment)
                        if (index >= 0)
                        {
                            recommendAdapter.mlist.removeAt(index)
                            recommendAdapter.notifyItemRemoved(index)
                        }
                    }
                    else
                    {
                        val index = commentAdapter.mlist.indexOf(comment)
                        if (index >= 0) {
                            commentAdapter.mlist.removeAt(index)
                            commentAdapter.notifyItemRemoved(index)
                        }
                    }
                    ToastUtil.showMessage("评论已删除")
                }
            },
            tvContent = "确定要删除这条评论吗",
            tvTitle = "提示",
            textNo = "取消",
            textYes = "删除"
        ).show()
    }

    /**
     * 调用系统分享（封面图片 + 文字 + 应用下载链接）
     */
    private fun shareBook() {
        val title = bookInfo?.bookTitle ?: "佛经分享"
        val intro = bookInfo?.bookIntro ?: ""
        val downloadUrl = GlobalConfig.versionInfo?.downloadUrl ?: ""
        val shareText = buildString {
            append("我正在阅读《$title》，推荐给你一起修行！\n")
            append(intro)
            if (downloadUrl.isNotEmpty()) {
                append("\n下载App：$downloadUrl")
            }
        }
        val coverUrl = bookInfo?.bookCover ?: ""

        Thread {
            //尝试下载封面到缓存目录(FileProvider声明的picture_photo/路径)
            val coverFile = try {
                val glideFile = Glide.with(this).asFile().load(coverUrl).submit().get()
                val dir = File(requireContext().cacheDir, "picture_photo").apply { mkdirs() }
                File(dir, "share_cover_${bookId}.jpg").apply {
                    glideFile.copyTo(this, overwrite = true)
                }
            } catch (e: Exception) {
                null
            }

            activity?.runOnUiThread {
                //用户在分享面板选中目标应用后的回调PendingIntent
                val chosenSender = createShareChosenSender()

                if (coverFile != null && coverFile.exists()) {
                    //图片 + 文字分享
                    val uri = FileProvider.getUriForFile(
                        requireContext(), "${requireContext().packageName}.fileprovider", coverFile
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER, chosenSender)
                    }
                    startActivity(Intent.createChooser(intent, "分享到"))
                } else {
                    //封面下载失败，降级为纯文本分享
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra(Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER, chosenSender)
                    }
                    startActivity(Intent.createChooser(intent, "分享到"))
                }
            }
        }.start()
    }

    /**
     * 创建分享目标选中回调的PendingIntent，由系统在用户选中分享目标后触发
     */
    private fun createShareChosenSender(): PendingIntent {
        val intent = Intent(ACTION_SHARE_CHOSEN).setPackage(requireContext().packageName)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        //API 31+ 必须声明MUTABLE，系统才能写入用户选中的组件信息
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(requireContext(), bookId, intent, flags)
    }

    override fun onDestroy() {
        runCatching { activity?.unregisterReceiver(shareChosenReceiver) }
        removeKeyboardListener()
        super.onDestroy()
    }

    /**
     * 显示评论输入框，弹出软键盘
     */
    private fun showCommentInput() {
        binding.clBottom.visibility = View.GONE
        binding.llInput.visibility = View.VISIBLE
        binding.etComment.isFocusable = true
        binding.etComment.isFocusableInTouchMode = true
        binding.etComment.requestFocus()
        binding.etComment.setSelection(binding.etComment.text?.length ?: 0)
        showSoftInput(binding.etComment)
    }

    /**
     * 隐藏评论输入框，收起软键盘
     */
    private fun hideCommentInput() {
        hideSoftInput()
        binding.llInput.visibility = View.INVISIBLE
        binding.clBottom.visibility = View.VISIBLE
        //重置输入框底部边距
        setInputBottomMargin(binding, 0)
    }

    /**
     * 监听键盘高度变化，手动调整 ll_input 位置
     * 因为 SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 导致 adjustResize 失效
     */
    private fun setupKeyboardListener() {
        val root = binding.root
        val observer = root.viewTreeObserver
        keyboardObserver = observer
        keyboardListener = ViewTreeObserver.OnGlobalLayoutListener {
            //视图销毁(mBinding 被置空)后该回调仍可能触发，这里判空避免空指针崩溃
            val b = mBinding as? FragFojingDetailsBinding ?: return@OnGlobalLayoutListener
            val rect = Rect()
            b.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = b.root.rootView.height
            val invisibleBottom = screenHeight - rect.bottom

            if (invisibleBottom > screenHeight / 4) {
                //键盘显示：用可视区域底部的位移量作为键盘高度，避免导航栏造成的偏差
                val keyboardHeight = lastVisibleBottom - rect.bottom
                if (keyboardHeight > 0) {
                    setInputBottomMargin(b, keyboardHeight)
                }
            } else {
                //键盘隐藏：记录基准位置并复位
                lastVisibleBottom = rect.bottom
                setInputBottomMargin(b, 0)
            }
        }
        observer.addOnGlobalLayoutListener(keyboardListener)
    }

    /**
     * 反注册键盘监听。
     * 使用注册时保存的 ViewTreeObserver，不依赖 binding，保证视图销毁后也能反注册成功
     */
    private fun removeKeyboardListener() {
        val observer = keyboardObserver
        val listener = keyboardListener
        if (observer != null && listener != null) {
            runCatching { observer.removeOnGlobalLayoutListener(listener) }
        }
        keyboardObserver = null
        keyboardListener = null
    }

    override fun onDestroyView() {
        //必须在 super.onDestroyView()（其中会把 mBinding 置空）之前反注册
        removeKeyboardListener()
        super.onDestroyView()
    }

    /**
     * 设置 ll_input 底部边距
     */
    private fun setInputBottomMargin(binding: FragFojingDetailsBinding, bottomMargin: Int) {
        val lp = binding.llInput.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        if (lp.bottomMargin != bottomMargin) {
            lp.bottomMargin = bottomMargin
            binding.llInput.layoutParams = lp
        }
    }

    /**
     * 弹出软键盘
     */
    override fun showSoftInput(view: View) {
        view.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    /**
     * 收起软键盘
     */
    override fun hideSoftInput() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etComment.windowToken, 0)
    }

    /**
     * 发送评论
     */
    private fun sendComment() {
        val text = binding.etComment.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            ToastUtil.showMessage("请输入评论内容")
            return
        }
        if (bookId <= 0) {
            ToastUtil.showMessage("书籍信息异常")
            return
        }
        mViewModel.addBookComment(bookId, text) { success,comment ->
            if (!isAdded) return@addBookComment
            if (success) {
                binding.etComment.setText("")
                hideCommentInput()
                ToastUtil.showMessage("发布成功")
                //刷新评论列表
                comment?.let {
                    if (GlobalConfig.userData?.roleType == GlobalConfig.ROLE_TYPE.ROLE_TYPE_MASTER)
                    {
                        recommendAdapter.addData(comment,0)
                    }
                    else
                    {
                        commentAdapter.addData(comment,0)
                    }
                }
                binding.btnAddComment.gone()
            } else {
                ToastUtil.showMessage("发布失败，请重试")
            }
        }
    }

    /**
     * 加入/移出书架
     */
    private fun switchShelf() {
        val book = bookInfo ?: return
        val addToShelf = !book.isInShelf
        if (addToShelf) {
            mViewModel.addToBookShelf(bookId) { success ->
                onShelfResult(success, true)
            }
        } else {
            mViewModel.removeFromBookShelf(bookId) { success ->
                onShelfResult(success, false)
            }
        }
    }

    /**
     * 书架操作结果
     */
    private fun onShelfResult(success: Boolean, addToShelf: Boolean) {
        if (!isAdded) return
        if (success) {
            bookInfo?.isInShelf = addToShelf
            refreshShelfBtn()
            ToastUtil.showMessage(if (addToShelf) "已加入书架" else "已移出书架")
        } else {
            ToastUtil.showMessage(if (addToShelf) "加入书架失败" else "移出书架失败")
        }
    }

    /**
     * 刷新书架按钮文案
     */
    private fun refreshShelfBtn() {
        binding.btnAddShelf.isSelected = bookInfo?.isInShelf ?: false
    }

    private fun refreshUI()
    {
        if (bookInfo == null) return
        binding.tvTitle.text = bookInfo?.bookTitle
        binding.ivBookCover.loadImg(bookInfo?.bookCover ?: "")
        binding.tvBookName.text = bookInfo?.bookTitle
        binding.ratingbar.rating = 5f
        binding.tvBookLevel.text = bookInfo?.bookType
        binding.tvBookCopy.text = "${bookInfo?.copyCount}佛友"
        binding.tvBookRead.text = "${bookInfo?.viewCount}佛友"
        binding.tvBookIntro.text = bookInfo?.bookIntro
        binding.tvRead.text = "已阅读 ${bookInfo?.readPercent}%"
        binding.tvCopy.text = "已抄写 ${bookInfo?.copyPercent}%"
        binding.btnAddComment.isVisible = !(bookInfo?.hasComment?:false)
        refreshShelfBtn()


        bookInfo?.masterRecommend?.let { masterRecommend ->
            recommendAdapter.initData(masterRecommend)
        }

    }
}
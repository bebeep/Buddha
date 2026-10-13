package com.fingertip.uilib.fragment.book

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.fingertip.baselib.bean.BuddhaBook
import com.fingertip.baselib.constant.GlobalConfig
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
        mViewModel.getBookInfoByBookId(bookId)
        mViewModel.getBookComment(bookId,currPage)
    }

    override fun getClickViews(): List<View> {
        return listOf(binding.vBack.ivBack, binding.ivShare, binding.tvRead, binding.tvCopy, binding.tvAddShelf)
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
            R.id.tv_add_shelf -> ToastUtil.showMessage("加入书架")
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
        recommendAdapter = FojingRecommendAdapter(requireContext()) { viewId, data ->
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
        }
        binding.rvRecommend.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecommend.adapter = recommendAdapter

        commentAdapter = FojingCommentAdapter(requireContext()){ viewId, data ->
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
        }
        binding.rvComment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComment.adapter = commentAdapter

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
        super.onDestroy()
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
        binding.tvAddShelf.isSelected = bookInfo?.isInShelf ?: false
        binding.tvAddShelf.text = if (bookInfo?.isInShelf ?: false) "已加入书架" else "加入书架"

        bookInfo?.masterRecommend?.let { masterRecommend ->
            recommendAdapter.initData(masterRecommend)
        }

    }
}
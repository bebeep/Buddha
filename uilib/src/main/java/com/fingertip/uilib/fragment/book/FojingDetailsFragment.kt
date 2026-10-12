package com.fingertip.uilib.fragment.book

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.BuddhaBook
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
import kotlin.math.abs
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

    override fun initShiTu() {
        bookId = arguments?.getInt(BOOK_ID, 0) ?: 0
        if (bookId <= 0) {
            return
        }
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
            R.id.iv_share -> ToastUtil.showMessage("分享")
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
                commentAdapter.mlist.addAll(it.data!!)
                commentAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onLoadMore(refreshLayout: RefreshLayout) {
        currPage ++
        mViewModel.getBookComment(bookId,currPage)
    }


    private fun initAdapter(){
        recommendAdapter = FojingRecommendAdapter(requireContext())
        binding.rvRecommend.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecommend.adapter = recommendAdapter

        commentAdapter = FojingCommentAdapter(requireContext())
        binding.rvComment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComment.adapter = commentAdapter

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

        bookInfo?.masterRecommend?.let { masterRecommend ->
            recommendAdapter.initData(masterRecommend)
        }

    }
}
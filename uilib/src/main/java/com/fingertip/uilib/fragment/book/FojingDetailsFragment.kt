package com.fingertip.uilib.fragment.book

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FojingCommentAdapter
import com.fingertip.uilib.adapter.FojingRecommendAdapter
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ColorUtil
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.databinding.FragFojingDetailsBinding
import com.google.android.material.appbar.AppBarLayout
import com.lzlz.toplib.extention.toPx
import com.scwang.smart.refresh.layout.SmartRefreshLayout
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

    lateinit var recommendAdapter: FojingRecommendAdapter
    lateinit var commentAdapter: FojingCommentAdapter

    override fun initShiTu() {

        initAdapter()

        binding.appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, scrollY ->
            val slideOffset = min(abs(scrollY * 1.0f / 100.toPx()),1f)
            binding.clTitle.setBackgroundColor(ColorUtil.changeAlpha(resources.getColor(R.color.white),slideOffset))
            binding.tvTitle.alpha = slideOffset
        })




        binding.srl.setOnLoadMoreListener(this)
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


    private fun initAdapter(){
        val v = requireView()
        recommendAdapter = FojingRecommendAdapter(requireContext())
        binding.rvRecommend.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecommend.adapter = recommendAdapter

        commentAdapter = FojingCommentAdapter(requireContext())
        binding.rvComment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComment.adapter = commentAdapter

        recommendAdapter.initData(listOf("","","",""))
        commentAdapter.initData(listOf("","","",""))
    }

    override fun onLoadMore(refreshLayout: RefreshLayout) {
        binding.srl.postDelayed({
            binding.srl.finishLoadMore()
            commentAdapter.addData(listOf("",""))
        },1000)
    }


}
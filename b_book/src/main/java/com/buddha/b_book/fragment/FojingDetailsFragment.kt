package com.buddha.b_book.fragment

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.buddha.b_book.R
import com.buddha.b_book.adapter.FojingCommentAdapter
import com.buddha.b_book.adapter.FojingRecommendAdapter
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ColorUtil
import com.fingertip.baselib.util.ToastUtil
import com.google.android.material.appbar.AppBarLayout
import com.lzlz.toplib.extention.toPx
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener
import kotlinx.android.synthetic.main.frag_fojing_details.*
import kotlin.math.abs
import kotlin.math.min

/**
 * 佛经-书籍详情
 */
class FojingDetailsFragment :TopVMFragment<BookshelfVM>(),OnLoadMoreListener{
    override fun layoutId() = R.layout.frag_fojing_details
    override fun initVM() = BookshelfVM()

    lateinit var recommendAdapter: FojingRecommendAdapter
    lateinit var commentAdapter: FojingCommentAdapter

    override fun initShiTu() {


        initAdapter()

        appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, scrollY ->
            val slideOffset = min(abs(scrollY * 1.0f / 100.toPx()),1f)
            cl_title.setBackgroundColor(ColorUtil.changeAlpha(resources.getColor(R.color.white),slideOffset))
            tv_title.alpha = slideOffset
        })




        srl.setOnLoadMoreListener(this)
    }

    override fun getClickViews() = listOf(v_back,iv_share,tv_read,tv_copy,tv_add_shelf)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v){
            v_back -> pop()
            iv_share -> ToastUtil.showMessage("分享")
            tv_read -> {
                start(FojingReadFragment())
            }
            tv_copy -> {
                ToastUtil.showMessage("抄经模式")
                start(FojingCopyFragment())
            }
            tv_add_shelf -> ToastUtil.showMessage("加入书架")
        }
    }


    private fun initAdapter(){
        recommendAdapter = FojingRecommendAdapter(requireContext())
        rv_recommend.layoutManager = LinearLayoutManager(requireContext())
        rv_recommend.adapter = recommendAdapter

        commentAdapter = FojingCommentAdapter(requireContext())
        rv_comment.layoutManager = LinearLayoutManager(requireContext())
        rv_comment.adapter = commentAdapter

        recommendAdapter.initData(listOf("","","",""))
        commentAdapter.initData(listOf("","","",""))
    }

    override fun onLoadMore(refreshLayout: RefreshLayout) {
        srl.postDelayed({
            srl.finishLoadMore()
            commentAdapter.addData(listOf("",""))
        },1000)
    }


}
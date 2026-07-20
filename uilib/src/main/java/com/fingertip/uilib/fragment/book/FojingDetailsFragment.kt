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
    override fun layoutId() = R.layout.frag_fojing_details
    override fun initVM() = BookshelfVM()

    lateinit var recommendAdapter: FojingRecommendAdapter
    lateinit var commentAdapter: FojingCommentAdapter

    override fun initShiTu() {
        val v = requireView()

        initAdapter()

        v.findViewById<AppBarLayout>(R.id.appbar).addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, scrollY ->
            val slideOffset = min(abs(scrollY * 1.0f / 100.toPx()),1f)
            v.findViewById<View>(R.id.cl_title).setBackgroundColor(ColorUtil.changeAlpha(resources.getColor(R.color.white),slideOffset))
            v.findViewById<TextView>(R.id.tv_title).alpha = slideOffset
        })




        v.findViewById<SmartRefreshLayout>(R.id.srl).setOnLoadMoreListener(this)
    }

    override fun getClickViews(): List<View> {
        val v = requireView()
        return listOf(v.findViewById(R.id.v_back), v.findViewById(R.id.iv_share), v.findViewById(R.id.tv_read), v.findViewById(R.id.tv_copy), v.findViewById(R.id.tv_add_shelf))
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
        v.findViewById<RecyclerView>(R.id.rv_recommend).layoutManager = LinearLayoutManager(requireContext())
        v.findViewById<RecyclerView>(R.id.rv_recommend).adapter = recommendAdapter

        commentAdapter = FojingCommentAdapter(requireContext())
        v.findViewById<RecyclerView>(R.id.rv_comment).layoutManager = LinearLayoutManager(requireContext())
        v.findViewById<RecyclerView>(R.id.rv_comment).adapter = commentAdapter

        recommendAdapter.initData(listOf("","","",""))
        commentAdapter.initData(listOf("","","",""))
    }

    override fun onLoadMore(refreshLayout: RefreshLayout) {
        requireView().findViewById<SmartRefreshLayout>(R.id.srl).postDelayed({
            requireView().findViewById<SmartRefreshLayout>(R.id.srl).finishLoadMore()
            commentAdapter.addData(listOf("",""))
        },1000)
    }


}
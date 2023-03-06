package com.buddha.b_book.fragment

import androidx.recyclerview.widget.GridLayoutManager
import com.buddha.b_book.R
import com.buddha.b_book.adapter.FojingAdapter
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ColorUtil
import com.google.android.material.appbar.AppBarLayout
import com.lzlz.toplib.extention.toPx
import kotlinx.android.synthetic.main.frag_fojing_subject_details.*
import kotlin.math.abs
import kotlin.math.min

/**
 * 佛经-书籍详情
 */
class FojingDetailsFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing_details
    override fun initVM() = BookshelfVM()

    lateinit var adapter: FojingAdapter

    override fun initShiTu() {


        initAdapter()

        appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, scrollY ->
            val slideOffset = min(abs(scrollY * 1.0f / 88.toPx()),1f)
            cl_title.setBackgroundColor(ColorUtil.changeAlpha(resources.getColor(R.color.white),slideOffset))
            tv_title.alpha = slideOffset
        })



        v_back.setOnClickListener {
            pop()
        }
    }


    private fun initAdapter(){
        adapter = FojingAdapter(requireContext())
        recyclerview.layoutManager = GridLayoutManager(requireContext(),3)
        recyclerview.adapter = adapter

        adapter.initData(listOf("","",""))
    }


}
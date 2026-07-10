package com.buddha.b_book.fragment

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.buddha.b_book.R
import com.buddha.b_book.adapter.FojingAdapter
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ColorUtil
import com.google.android.material.appbar.AppBarLayout
import com.lzlz.toplib.extention.toPx
import kotlin.math.abs
import kotlin.math.min

/**
 * 佛经-专题详情
 */
class FojingSubjectDetailsFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing_subject_details
    override fun initVM() = BookshelfVM()

    lateinit var adapter: FojingAdapter

    override fun initShiTu() {
        val v = requireView()

        initAdapter()

        v.findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appbar).addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, scrollY ->
            val slideOffset = min(abs(scrollY * 1.0f / 88.toPx()),1f)
            v.findViewById<View>(R.id.cl_title).setBackgroundColor(ColorUtil.changeAlpha(resources.getColor(R.color.white),slideOffset))
            v.findViewById<android.widget.TextView>(R.id.tv_title).alpha = slideOffset
        })



        v.findViewById<View>(R.id.v_back).setOnClickListener {
            pop()
        }
    }


    private fun initAdapter(){
        adapter = FojingAdapter(requireContext())
        val rv = requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerview)
        rv.layoutManager = GridLayoutManager(requireContext(),3)
        rv.adapter = adapter

        adapter.initData(listOf("","",""))
    }


}
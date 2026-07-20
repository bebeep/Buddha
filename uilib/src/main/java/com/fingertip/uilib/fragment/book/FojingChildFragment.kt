package com.fingertip.uilib.fragment.book

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FojingAdapter
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment

/**
 * 佛经-分类
 */
class FojingChildFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing_child
    override fun initVM() = BookshelfVM()

    lateinit var adapter: FojingAdapter
    override fun initShiTu() {
        initAdapter()
    }


    private fun initAdapter(){
        adapter = FojingAdapter(requireContext())
        val rv = requireView().findViewById<RecyclerView>(R.id.recyclerview)
        rv.layoutManager = GridLayoutManager(requireContext(),3)
        rv.adapter = adapter

        adapter.initData(listOf("","","","","","","",""))
    }


}
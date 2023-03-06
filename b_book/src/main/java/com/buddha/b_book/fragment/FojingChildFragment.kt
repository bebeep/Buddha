package com.buddha.b_book.fragment

import androidx.recyclerview.widget.GridLayoutManager
import com.buddha.b_book.R
import com.buddha.b_book.adapter.FojingAdapter
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import kotlinx.android.synthetic.main.frag_fojing_child.*

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
        recyclerview.layoutManager = GridLayoutManager(requireContext(),3)
        recyclerview.adapter = adapter

        adapter.initData(listOf("","","","","","","",""))
    }


}
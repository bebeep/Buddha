package com.fingertip.uilib.fragment.book

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.BookShelfAdapter
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment

/**
 * 书架
 */
class BookShelfFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_book_shelf
    override fun initVM() = BookshelfVM()

    lateinit var adapter: BookShelfAdapter

    override fun initShiTu() {
        initAdapter()

        adapter.initData(listOf("","","","","","",""))
    }


    private fun initAdapter(){
        adapter = BookShelfAdapter(requireContext())
        val rv = requireView().findViewById<RecyclerView>(R.id.recyclerview)
        rv.layoutManager = GridLayoutManager(requireContext(),3)
        rv.adapter = adapter
    }

}
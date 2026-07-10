package com.buddha.b_book.fragment

import androidx.recyclerview.widget.GridLayoutManager
import com.buddha.b_book.R
import com.buddha.b_book.adapter.BookShelfAdapter
import com.buddha.b_book.vm.BookshelfVM
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
        val rv = requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerview)
        rv.layoutManager = GridLayoutManager(requireContext(),3)
        rv.adapter = adapter
    }

}
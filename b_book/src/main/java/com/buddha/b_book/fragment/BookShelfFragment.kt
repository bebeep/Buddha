package com.buddha.b_book.fragment

import androidx.recyclerview.widget.GridLayoutManager
import com.buddha.b_book.R
import com.buddha.b_book.adapter.BookShelfAdapter
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import kotlinx.android.synthetic.main.frag_book_shelf.*

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
        recyclerview.layoutManager = GridLayoutManager(requireContext(),3)
        recyclerview.adapter = adapter
    }

}
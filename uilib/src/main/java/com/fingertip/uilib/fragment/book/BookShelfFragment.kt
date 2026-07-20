package com.fingertip.uilib.fragment.book

import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.BookShelfAdapter
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.databinding.FragBookShelfBinding

/**
 * 书架
 */
class BookShelfFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_book_shelf
    override fun initVM() = BookshelfVM()

    private val binding get() = mBinding as FragBookShelfBinding

    lateinit var adapter: BookShelfAdapter

    override fun initShiTu() {
        initAdapter()

        adapter.initData(listOf("","","","","","",""))
    }


    private fun initAdapter(){
        adapter = BookShelfAdapter(requireContext())
        binding.recyclerview.layoutManager = GridLayoutManager(requireContext(),3)
        binding.recyclerview.adapter = adapter
    }

}
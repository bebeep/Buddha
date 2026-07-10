package com.buddha.b_book.fragment

import androidx.recyclerview.widget.LinearLayoutManager
import com.buddha.b_book.R
import com.buddha.b_book.adapter.SubjectAdapter
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment

/**
 * 佛经-专题
 */
class FojingSubjectFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing_subject
    override fun initVM() = BookshelfVM()

    lateinit var adapter: SubjectAdapter
    override fun initShiTu() {
        initAdapter()
    }


    private fun initAdapter(){
        adapter = SubjectAdapter(requireContext()){
            start(FojingSubjectDetailsFragment())
        }
        val rv = requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerview)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        adapter.initData(listOf("","","","","","","",""))
    }


}
package com.buddha.b_book.fragment

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.buddha.b_book.R
import com.buddha.b_book.adapter.SubjectAdapter
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import kotlinx.android.synthetic.main.frag_fojing_subject.*

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
            startActRootFragment(FojingSubjectDetailsFragment())
        }
        recyclerview.layoutManager = LinearLayoutManager(requireContext())
        recyclerview.adapter = adapter

        adapter.initData(listOf("","","","","","","",""))
    }


}
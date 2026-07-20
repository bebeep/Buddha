package com.fingertip.uilib.fragment.book

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.SubjectAdapter
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.databinding.FragFojingSubjectBinding

/**
 * 佛经-专题
 */
class FojingSubjectFragment :TopVMFragment<BookshelfVM>(){
    private val binding get() = mBinding as FragFojingSubjectBinding
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
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = adapter

        adapter.initData(listOf("","","","","","","",""))
    }


}
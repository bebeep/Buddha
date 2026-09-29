package com.fingertip.uilib.fragment.book

import androidx.recyclerview.widget.LinearLayoutManager
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

        binding.srl.setOnRefreshListener {
            mViewModel.getBookThemeList()
        }

        mViewModel.getBookThemeList()
    }


    private fun initAdapter(){
        adapter = SubjectAdapter(requireContext()){
            (parentFragment?.parentFragment as BuddhaTextsFragment).start(FojingSubjectDetailsFragment())
        }
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = adapter
    }


    override fun initObserver() {
        super.initObserver()
        mViewModel.bookSubjectResult.observe(this){
            binding.srl.isRefreshing = false
            if (it.success)
            {
                adapter.initData(it.data!!)
            }
        }
    }
}
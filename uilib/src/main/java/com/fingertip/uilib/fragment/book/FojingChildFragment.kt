package com.fingertip.uilib.fragment.book

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FojingAdapter
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.databinding.FragFojingChildBinding
import com.fingertip.uilib.fragment.moment.MomentChildFragment.Companion.MOMENT
import com.fingertip.uilib.fragment.moment.MomentChildFragment.Companion.TYPE_STRING

/**
 * 佛经-分类
 */
class FojingChildFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing_child
    override fun initVM() = BookshelfVM()

    private val binding get() = mBinding as FragFojingChildBinding

    lateinit var adapter: FojingAdapter


    companion object {
        const val TYPE_ID = "TYPE_ID"
        fun newInstance(bookType: Int): FojingChildFragment {
            return FojingChildFragment().apply {
                arguments = Bundle().apply {
                    putInt(TYPE_ID, bookType)
                }
            }
        }
    }

    var bookType: Int = 0


    override fun onNewBundle(args: Bundle?) {
        super.onNewBundle(args)
        initShiTu()
    }

    override fun initShiTu() {
        bookType = arguments?.getInt(TYPE_ID, 0) ?: 0
        if (bookType <= 0) {
            return
        }
        initAdapter()
        binding.srl.setOnRefreshListener {
            mViewModel.getBookByType(bookType, 0)
        }
        mViewModel.getBookByType(bookType, 0)
    }


    private fun initAdapter(){
        adapter = FojingAdapter(requireContext())
        binding.recyclerview.layoutManager = GridLayoutManager(requireContext(),3)
        binding.recyclerview.adapter = adapter

    }

    override fun initObserver() {
        super.initObserver()
        mViewModel.bookListResult.observe(this){
            binding.srl.isRefreshing = false
            if (it.success)
            {
                adapter.initData(it.data)
            }
        }
    }

}
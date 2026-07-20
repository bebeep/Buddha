package com.fingertip.uilib.fragment.book

import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.databinding.FragFojingBinding

/**
 * 佛经
 */
class FojingFragment :TopVMFragment<BookshelfVM>(){

    override fun layoutId() = R.layout.frag_fojing
    override fun initVM() = BookshelfVM()

    private val binding get() = mBinding as FragFojingBinding


    override fun initShiTu() {
        binding.vp.postDelayed({
            binding.vp.offscreenPageLimit = 5
            binding.vp.adapter = TopFragmentPagerAdapter(listOf(
                FojingSubjectFragment(),
                FojingChildFragment(),
                FojingChildFragment(),
                FojingChildFragment(),
                FojingChildFragment()
            ), childFragmentManager)
            binding.tabLayout.setViewPager(binding.vp, mutableListOf("专题","经典","入门","进阶","研究").toTypedArray())
        }, 600)


    }



}
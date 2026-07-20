package com.fingertip.uilib.fragment.book

import androidx.viewpager.widget.ViewPager
import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.databinding.FragFojingBinding
import com.flyco.tablayout.SlidingTabLayout

/**
 * 佛经
 */
class FojingFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing
    override fun initVM() = BookshelfVM()

    lateinit var binding: FragFojingBinding

    override fun initShiTu() {
        binding = FragFojingBinding.inflate(layoutInflater)
//        binding.vp.postDelayed({
//            binding.vp.offscreenPageLimit = 5
//            binding.vp.adapter = TopFragmentPagerAdapter(listOf(
//                FojingSubjectFragment(),
//                FojingChildFragment(),
//                FojingChildFragment(),
//                FojingChildFragment(),
//                FojingChildFragment()
//            ), childFragmentManager)
//            binding.tabLayout.setViewPager(binding.vp, mutableListOf("专题","经典","入门","进阶","研究").toTypedArray())
//        }, 100)

        binding.vp.offscreenPageLimit = 5
        binding.vp.adapter = TopFragmentPagerAdapter(listOf(
            FojingSubjectFragment(),
            FojingChildFragment(),
            FojingChildFragment(),
            FojingChildFragment(),
            FojingChildFragment()
        ), childFragmentManager)
        binding.tabLayout.setViewPager(binding.vp, mutableListOf("专题","经典","入门","进阶","研究").toTypedArray())
    }



}
package com.buddha.b_book.fragment

import com.buddha.b_book.R
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopVMFragment

/**
 * 佛经
 */
class FojingFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing
    override fun initVM() = BookshelfVM()

    override fun initShiTu() {
        val v = requireView()
        val vpView = v.findViewById<androidx.viewpager.widget.ViewPager>(R.id.vp)
        vpView.offscreenPageLimit = 5
        vpView.adapter = TopFragmentPagerAdapter(listOf(FojingSubjectFragment(),FojingChildFragment(),FojingChildFragment(),FojingChildFragment(),FojingChildFragment()), childFragmentManager)
        v.findViewById<com.flyco.tablayout.SlidingTabLayout>(R.id.tab_layout).setViewPager(vpView, mutableListOf("专题","经典","入门","进阶","研究").toTypedArray())
    }


}
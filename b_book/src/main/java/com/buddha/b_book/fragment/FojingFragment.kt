package com.buddha.b_book.fragment

import com.buddha.b_book.R
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopVMFragment
import kotlinx.android.synthetic.main.frag_fojing.*

/**
 * 佛经
 */
class FojingFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing
    override fun initVM() = BookshelfVM()

    override fun initShiTu() {


        vp.offscreenPageLimit = 5
        vp.adapter = TopFragmentPagerAdapter(listOf(FojingSubjectFragment(),FojingChildFragment(),FojingChildFragment(),FojingChildFragment(),FojingChildFragment()), childFragmentManager)
        tab_layout.setViewPager(vp, mutableListOf("专题","经典","入门","进阶","研究").toTypedArray())
    }


}
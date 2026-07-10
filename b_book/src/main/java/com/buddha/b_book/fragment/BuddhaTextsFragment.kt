package com.buddha.b_book.fragment

import android.view.View
import com.buddha.b_book.R
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.view.tablayout.ScaleTabLayout
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopVMFragment

/**
 * 佛经
 */
class BuddhaTextsFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_buddha_texts
    override fun initVM() = BookshelfVM()




    override fun initShiTu() {
        val v = requireView()
        val vpView = v.findViewById<androidx.viewpager.widget.ViewPager>(R.id.vp)
        vpView.offscreenPageLimit = 2
        vpView.adapter = TopFragmentPagerAdapter(listOf(BookShelfFragment(),FojingFragment()), childFragmentManager)

        v.findViewById<ScaleTabLayout>(R.id.tab_layout).setViewPager(vpView, mutableListOf("书架","佛经").toTypedArray())


        v.findViewById<View>(R.id.v_back).setOnClickListener {
            pop()
        }

    }


}
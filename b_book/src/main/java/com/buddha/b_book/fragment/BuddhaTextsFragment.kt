package com.buddha.b_book.fragment

import com.buddha.b_book.R
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopVMFragment
import kotlinx.android.synthetic.main.frag_buddha_texts.*

/**
 * 佛经
 */
class BuddhaTextsFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_buddha_texts
    override fun initVM() = BookshelfVM()




    override fun initShiTu() {

        vp.offscreenPageLimit = 2
        vp.adapter = TopFragmentPagerAdapter(listOf(BookShelfFragment(),FojingFragment()), childFragmentManager)

        tab_layout.setViewPager(vp, mutableListOf("书架","佛经").toTypedArray())


        v_back.setOnClickListener {
            pop()
        }

    }


}
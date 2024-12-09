package com.fingertip.uilib.fragment.message

import com.buddha.b_book.fragment.FojingChildFragment
import com.buddha.b_book.fragment.FojingSubjectFragment
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.MainVM
import kotlinx.android.synthetic.main.fragment_message.*

class MessageFragment:TopPmFragment<MainVM>() {
    override fun initVM() = MainVM()
    override fun layoutId() = R.layout.fragment_message

    override fun initShiTu() {
        vp.offscreenPageLimit = 3
        vp.adapter = TopFragmentPagerAdapter(listOf(
            MessageChildFragment(),
            MessageSystemFragment()
        ), childFragmentManager)
        tab_layout.setViewPager(vp, mutableListOf("与我相关","系统消息").toTypedArray())
    }
}
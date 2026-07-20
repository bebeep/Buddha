package com.fingertip.uilib.fragment.message

import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.MainVM

class MessageFragment:TopPmFragment<MainVM>() {
    override fun initVM() = MainVM()
    override fun layoutId() = R.layout.fragment_message

    override fun initShiTu() {
        val v = requireView()
        val vpView = v.findViewById<androidx.viewpager.widget.ViewPager>(R.id.vp)
        vpView.offscreenPageLimit = 3
        vpView.adapter = TopFragmentPagerAdapter(listOf(
            MessageChildFragment(),
            MessageSystemFragment()
        ), childFragmentManager)
        v.findViewById<com.flyco.tablayout.SlidingTabLayout>(R.id.tab_layout).setViewPager(vpView, mutableListOf("与我相关","系统消息").toTypedArray())
    }
}
package com.fingertip.uilib.fragment.message

import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentMessageBinding
import com.fingertip.uilib.viewmodel.MainVM

class MessageFragment:TopPmFragment<MainVM>() {
    override fun initVM() = MainVM()
    override fun layoutId() = R.layout.fragment_message

    private val binding get() = mBinding as FragmentMessageBinding


    override fun initShiTu() {
        binding.vp.postDelayed({
            binding.vp.offscreenPageLimit = 2
            binding.vp.adapter = TopFragmentPagerAdapter(listOf(
                MessageChildFragment(),
                MessageSystemFragment()
            ), childFragmentManager)
            binding.tabLayout.setViewPager(binding.vp, mutableListOf("与我相关","系统消息").toTypedArray())
        },350)
    }
}
package com.fingertip.baselib.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.weikaiyun.fragmentation.SupportFragment

class TopFragmentPagerAdapter(
    private val fragments: List<SupportFragment>,
    manager: FragmentManager
): FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment = fragments[position]

    override fun getCount(): Int = fragments.size
}
package com.fingertip.uilib.fragment.book

import android.view.View
import androidx.viewpager.widget.ViewPager
import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.view.tablayout.ScaleTabLayout
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.databinding.FragBuddhaTextsBinding

/**
 * 佛经
 */
class BuddhaTextsFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_buddha_texts
    override fun initVM() = BookshelfVM()

    private val binding get() = mBinding as FragBuddhaTextsBinding


    override fun initShiTu() {

        binding.vp.offscreenPageLimit = 2
        binding.vp.adapter = TopFragmentPagerAdapter(listOf(BookShelfFragment(), FojingFragment()), childFragmentManager)

        binding.tabLayout.setViewPager(binding.vp, mutableListOf("书架","佛经").toTypedArray())


        binding.vIncludeBack.ivBack.setOnClickListener {
            pop()
        }

    }


}
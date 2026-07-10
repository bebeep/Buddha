package com.fingertip.uilib.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentMainBinding
import com.fingertip.uilib.fragment.moment.MomentFragment
import com.fingertip.uilib.fragment.worshiping.WorshipFragment
import com.fingertip.uilib.viewmodel.MainVM
import com.fingertip.uilib.widgets.bottom_menu.BottomMenu
import com.fingertip.uilib.widgets.bottom_menu.BottomMenuItem
import com.lzlz.toplib.extention.gone
import com.lzlz.toplib.extention.visible
import com.weikaiyun.fragmentation.SupportFragment
import org.greenrobot.eventbus.Subscribe

import com.fingertip.uilib.fragment.HostFragment
import com.fingertip.uilib.fragment.RankFragment
import com.fingertip.uilib.fragment.MeFragment

/**
 * 主Fragment
 */
class MainFragment : TopVMFragment<MainVM>(), BottomMenu.MenuSelectCallback {

    override fun initVM() = MainVM()

    override fun layoutId() = R.layout.fragment_main

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private val fragmentList = ArrayList<SupportFragment>()
    private var lastFragment: SupportFragment? = null

    override fun initShiTu() {

        if (findChildFragment(HostFragment::class.java) != null) {
            fragmentList.add(findChildFragment(HostFragment::class.java))
            fragmentList.add(findChildFragment(MomentFragment::class.java))
            fragmentList.add(findChildFragment(WorshipFragment::class.java))
            fragmentList.add(findChildFragment(RankFragment::class.java))
            fragmentList.add(findChildFragment(MeFragment::class.java))
        } else {
            fragmentList.add(HostFragment())
            fragmentList.add(MomentFragment())
            fragmentList.add(WorshipFragment())
            fragmentList.add(RankFragment())
            fragmentList.add(MeFragment())

            loadMultipleRootFragment(R.id.multi_container, 0, *fragmentList.toTypedArray())
        }

        lastFragment = fragmentList[0]

        binding.bottomMenu.also {
            it.menuSelectCallback = this
            it.addMenuItem(BottomMenuItem(R.mipmap.icon_menu, true))
            it.addMenuItem(BottomMenuItem(R.mipmap.icon_menu))
            it.addMenuItem(BottomMenuItem(R.mipmap.icon_menu))
            it.addMenuItem(BottomMenuItem(R.mipmap.icon_menu, hasMsg = true))
            it.addMenuItem(BottomMenuItem(R.mipmap.icon_menu))
        }


    }


    override fun initObserver() {

    }

    var lastIndex = 0
    override fun onMenuSelect(pos: Int) {
        showHideFragment(fragmentList[pos], lastFragment)
        lastFragment = fragmentList[pos]
        lastIndex = pos
        if (pos == 2) {
            binding.multiContainer.gone()
            binding.bottomMenu.alpha = 0.2f
        } else {
            binding.multiContainer.visible()
            binding.bottomMenu.alpha = 1f
        }
    }


    @Subscribe
    fun onMessageEvent(event: MessageEvent) {

    }
}
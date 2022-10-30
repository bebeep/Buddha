package com.fingertip.uilib.fragment

import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.MainVM
import com.fingertip.uilib.widgets.bottom_menu.BottomMenu
import com.fingertip.uilib.widgets.bottom_menu.BottomMenuItem
import kotlinx.android.synthetic.main.fragment_main.*
import me.yokeyword.fragmentation.SupportFragment
import org.greenrobot.eventbus.Subscribe

/**
 * 主Fragment
 */
class MainFragment : TopVMFragment<MainVM>(), BottomMenu.MenuSelectCallback {

    override fun initVM()  = MainVM()

    override fun layoutId() = R.layout.fragment_main

    private val fragmentList = ArrayList<SupportFragment>()
    private var lastFragment: SupportFragment? = null

    override fun initShiTu() {

        if (findChildFragment(HostFragment::class.java) != null) {
            // 这里库已经做了Fragment恢复, 所有不需要额外的处理了, 不会出现重叠问题
            // 这里我们需要拿到mFragments的引用
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

        bottom_menu.menuSelectCallback = this

        bottom_menu.addMenuItem(BottomMenuItem(R.drawable.tab_me_selector, true))
        bottom_menu.addMenuItem(BottomMenuItem(R.drawable.tab_me_selector))
        bottom_menu.addMenuItem(BottomMenuItem(R.drawable.tab_me_selector))
        bottom_menu.addMenuItem(BottomMenuItem(R.drawable.tab_me_selector, hasMsg = true))
        bottom_menu.addMenuItem(BottomMenuItem(R.drawable.tab_me_selector))


    }


    override fun initObserver() {

    }

    var lastIndex = 0
    override fun onMenuSelect(pos: Int) {
        showHideFragment(fragmentList[pos], lastFragment)
        lastFragment = fragmentList[pos]
        lastIndex = pos
    }


    @Subscribe
    fun onMessageEvent(event: MessageEvent) {

    }
}
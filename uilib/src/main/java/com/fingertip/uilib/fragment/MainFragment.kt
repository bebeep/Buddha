package com.fingertip.uilib.fragment

import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.fragment.moment.MomentFragment
import com.fingertip.uilib.fragment.worshiping.WorshipFragment
import com.fingertip.uilib.viewmodel.MainVM
import com.fingertip.uilib.widgets.bottom_menu.BottomMenu
import com.fingertip.uilib.widgets.bottom_menu.BottomMenuItem
import com.lzlz.toplib.extention.gone
import com.lzlz.toplib.extention.visible
import com.weikaiyun.fragmentation.SupportFragment
import kotlinx.android.synthetic.main.fragment_main.*
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

        bottom_menu.addMenuItem(BottomMenuItem(R.drawable.tab_host_selector,true))
        bottom_menu.addMenuItem(BottomMenuItem(R.drawable.tab_post_selector))
        bottom_menu.addMenuItem(BottomMenuItem(R.drawable.tab_baifo_selector))
        bottom_menu.addMenuItem(BottomMenuItem(R.drawable.tab_rank_selector, hasMsg = true))
        bottom_menu.addMenuItem(BottomMenuItem(R.drawable.tab_me_selector))


    }


    override fun initObserver() {

    }

    var lastIndex = 0
    override fun onMenuSelect(pos: Int) {
        showHideFragment(fragmentList[pos], lastFragment)
        lastFragment = fragmentList[pos]
        lastIndex = pos
        if(pos == 2){
            multi_container.gone()
            bottom_menu.alpha = 0.2f
        }else{
            multi_container.visible()
            bottom_menu.alpha = 1f
        }
    }


    @Subscribe
    fun onMessageEvent(event: MessageEvent) {

    }
}
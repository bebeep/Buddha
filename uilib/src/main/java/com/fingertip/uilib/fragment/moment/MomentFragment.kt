package com.fingertip.uilib.fragment.moment

import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopFragment
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.uilib.R
import com.weikaiyun.fragmentation.SupportFragment
import kotlinx.android.synthetic.main.fragment_moment.*
import org.greenrobot.eventbus.Subscribe

/**
 * 佛友圈
 */
class MomentFragment : TopFragment() {
    override fun layoutId(): Int = R.layout.fragment_moment

    override fun initShiTu() {
        val titles = mutableListOf("佛友圈","关注")
        val fragments = mutableListOf<SupportFragment>(
            MomentChildFragment.newInstance(MomentChildFragment.MOMENT),
            MomentChildFragment.newInstance(MomentChildFragment.FOLLOW)
        )

        vp.offscreenPageLimit = fragments.size
        vp.adapter = TopFragmentPagerAdapter(fragments, childFragmentManager)

        tab_layout.textBold = 2
        tab_layout.setViewPager(vp, titles.toTypedArray())

    }

    override fun onVisible() {
        super.onVisible()
        refreshData()

    }


    private fun refreshData() {
    }



    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when(event.what) {

        }
    }
}
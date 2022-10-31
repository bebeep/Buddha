package com.fingertip.uilib.fragment.moment

import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopFragment
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.uilib.R
import kotlinx.android.synthetic.main.fragment_moment.*
import me.yokeyword.fragmentation.SupportFragment
import org.greenrobot.eventbus.Subscribe

/**
 * 佛友圈
 */
class MomentFragment : TopFragment() {
    override fun layoutId(): Int = R.layout.fragment_moment

    override fun initShiTu() {
        val titles = mutableListOf("关注","佛友圈")
        val fragments = mutableListOf<SupportFragment>(
            MomentChildFragment.newInstance(MomentChildFragment.FOLLOW),
            MomentChildFragment.newInstance(MomentChildFragment.MOMENT)
        )

        vp.offscreenPageLimit = fragments.size
        vp.currentItem = 1
        vp.adapter = TopFragmentPagerAdapter(fragments, childFragmentManager)

        tab_layout.setViewPager(vp, titles.toTypedArray())

    }

    override fun onSupportVisible() {
        super.onSupportVisible()
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
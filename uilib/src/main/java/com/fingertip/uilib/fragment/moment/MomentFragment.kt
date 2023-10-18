package com.fingertip.uilib.fragment.moment

import android.view.View
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopFragment
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.fragment.MainFragment
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


    override fun getClickViews() = listOf(iv_search,iv_notify,iv_moment_post)


    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v){
            iv_search->{}
            iv_notify->{}
            iv_moment_post->{
                (parentFragment as? MainFragment)?.start(MomentPostFragment())
            }
        }
    }



}
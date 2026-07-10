package com.fingertip.uilib.fragment.moment

import android.view.View
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.view.tablayout.GradualTabLayout
import com.fingertip.baselib.top.TopFragment
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.fragment.MainFragment
import com.fingertip.uilib.fragment.message.MessageFragment
import com.fingertip.uilib.fragment.search.SearchFragment
import com.weikaiyun.fragmentation.SupportFragment
import org.greenrobot.eventbus.Subscribe

/**
 * 佛友圈
 */
class MomentFragment : TopFragment() {
    override fun layoutId(): Int = R.layout.fragment_moment

    override fun initShiTu() {
        val v = requireView()
        val vpView = v.findViewById<androidx.viewpager.widget.ViewPager>(R.id.vp)
        val tabLayout = v.findViewById<GradualTabLayout>(R.id.tab_layout)
        val titles = mutableListOf("佛友圈","关注")
        val fragments = mutableListOf<SupportFragment>(
            MomentChildFragment.newInstance(MomentChildFragment.MOMENT),
            MomentChildFragment.newInstance(MomentChildFragment.FOLLOW)
        )

        vpView.offscreenPageLimit = fragments.size
        vpView.adapter = TopFragmentPagerAdapter(fragments, childFragmentManager)

        tabLayout.textBold = 2
        tabLayout.setViewPager(vpView, titles.toTypedArray())

    }


    override fun getClickViews(): List<View> {
        val v = requireView()
        return listOf(v.findViewById(R.id.iv_search), v.findViewById(R.id.iv_notify), v.findViewById(R.id.iv_moment_post))
    }


    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v?.id){
            R.id.iv_search->{
                (parentFragment as? MainFragment)?.start(SearchFragment())
            }
            R.id.iv_notify->{
                (parentFragment as? MainFragment)?.start(MessageFragment())
            }
            R.id.iv_moment_post->{
                (parentFragment as? MainFragment)?.start(MomentPostFragment())
            }
        }
    }



}
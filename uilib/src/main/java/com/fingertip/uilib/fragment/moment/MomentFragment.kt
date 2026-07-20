package com.fingertip.uilib.fragment.moment

import android.view.View
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.view.tablayout.GradualTabLayout
import com.fingertip.baselib.top.TopFragment
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentMomentBinding
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

    private val binding get() = mBinding as FragmentMomentBinding

    override fun initShiTu() {

        val titles = mutableListOf("佛友圈","关注")
        val fragments = mutableListOf<SupportFragment>(
            MomentChildFragment.newInstance(MomentChildFragment.MOMENT),
            MomentChildFragment.newInstance(MomentChildFragment.FOLLOW)
        )

        binding.vp.offscreenPageLimit = fragments.size
        binding.vp.adapter = TopFragmentPagerAdapter(fragments, childFragmentManager)

        binding.tabLayout.textBold = 2
        binding.tabLayout.setViewPager(binding.vp, titles.toTypedArray())

    }


    override fun getClickViews(): List<View> {
        return listOf(binding.ivSearch, binding.ivNotify, binding.ivMomentPost)
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
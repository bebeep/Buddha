package com.fingertip.uilib.fragment.moment

import android.view.View
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.view.tablayout.GradualTabLayout
import com.fingertip.baselib.top.TopFragment
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentMomentBinding
import com.fingertip.uilib.fragment.message.MessageFragment
import com.fingertip.uilib.fragment.search.SearchFragment
import com.fingertip.uilib.utils.openPage
import com.weikaiyun.fragmentation.SupportFragment
import org.greenrobot.eventbus.Subscribe

/**
 * 佛友圈
 */
class MomentFragment : TopFragment() {
    override fun layoutId(): Int = R.layout.fragment_moment

    private val binding get() = mBinding as FragmentMomentBinding

    override fun initShiTu() {

        //四分流（§12.6）：关注 / 推荐 / 共修打卡 / 最新
        val titles = mutableListOf("关注","推荐","共修打卡","最新")
        val fragments = mutableListOf<SupportFragment>(
            MomentChildFragment.newInstance(MomentChildFragment.FOLLOW),
            MomentChildFragment.newInstance(MomentChildFragment.MOMENT),
            MomentChildFragment.newInstance(MomentChildFragment.CHECKIN),
            MomentChildFragment.newInstance(MomentChildFragment.NEW)
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
                openPage(SearchFragment())
            }
            R.id.iv_notify->{
                openPage(MessageFragment())
            }
            R.id.iv_moment_post->{
                openPage(MomentPostFragment())
            }
        }
    }



}
package com.fingertip.uilib.fragment.user

import android.view.View
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentFansFollowListBinding
import com.fingertip.uilib.viewmodel.FansFollowVM

/**
 * 粉丝 / 关注列表容器页：两个 tab 支持点击与左右滑动切换。
 *
 * tab 点击与滑动共用 ViewPager 一套状态；每个分页自带分页与空态，
 * 互不干扰。只查当前登录人的粉丝/关注，不展示他人关系列表。
 */
class FansFollowListFragment : TopPmFragment<FansFollowVM>() {

    companion object {
        const val TAB_FANS = 0
        const val TAB_FOLLOWING = 1

        private const val ARG_TAB = "arg_tab"

        /**
         * @param tab 进页默认落在哪个 tab（粉丝 0 / 关注 1）
         */
        fun newInstance(tab: Int = TAB_FANS): FansFollowListFragment {
            return FansFollowListFragment().apply {
                arguments = android.os.Bundle().apply {
                    putInt(ARG_TAB, tab)
                }
            }
        }
    }

    override fun layoutId(): Int = R.layout.fragment_fans_follow_list
    override fun initVM(): FansFollowVM = provideVM()

    private val binding get() = mBinding as FragmentFansFollowListBinding

    override fun initShiTu() {
        val tab = arguments?.getInt(ARG_TAB, TAB_FANS) ?: TAB_FANS

        val titles = arrayOf(
            getString(R.string.fans_follow_tab_fans),
            getString(R.string.fans_follow_tab_following)
        )
        //只缓存相邻一页：两页各自分页，预加载一页足够顺滑
        binding.vpFansFollow.offscreenPageLimit = 1
        binding.vpFansFollow.adapter = TopFragmentPagerAdapter(
            listOf(
                FansFollowTabFragment.newInstance(TAB_FANS),
                FansFollowTabFragment.newInstance(TAB_FOLLOWING)
            ),
            childFragmentManager
        )
        //与收藏页统一字号，避免 ScaleTabLayout 默认 16/18 在两 tab 下显得太重
        binding.tabLayout.normalTextSize = 13f
        binding.tabLayout.selectTextSize = 14f
        binding.tabLayout.setViewPager(binding.vpFansFollow, titles)
        binding.vpFansFollow.currentItem = tab
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        if (v == binding.ivBack) pop()
    }
}

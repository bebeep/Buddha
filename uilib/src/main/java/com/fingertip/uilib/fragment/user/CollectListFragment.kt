package com.fingertip.uilib.fragment.user

import android.view.View
import com.fingertip.baselib.bean.CollectData
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentCollectBinding
import com.fingertip.uilib.viewmodel.UserToolVM

/**
 * 我的收藏：四个类型分页可左右滑动，tab 点击与滑动都走 ViewPager 一套状态。
 * 每个分页自己管分页和空态，取消收藏后由事件总线同步摘条目。
 */
class CollectListFragment : TopPmFragment<UserToolVM>() {

    override fun layoutId() = R.layout.fragment_collect
    override fun initVM(): UserToolVM = provideVM()

    private val binding get() = mBinding as FragmentCollectBinding

    override fun initShiTu() {
        val types = listOf(
            CollectData.TYPE_ALL, CollectData.TYPE_SUTRA,
            CollectData.TYPE_MOMENT, CollectData.TYPE_TEMPLE
        )
        val titles = arrayOf(
            getString(R.string.collect_tab_all),
            getString(R.string.collect_tab_sutra),
            getString(R.string.collect_tab_moment),
            getString(R.string.collect_tab_temple)
        )
        binding.vpCollect.offscreenPageLimit = types.size - 1
        binding.vpCollect.adapter = TopFragmentPagerAdapter(
            types.map { CollectTabFragment.newInstance(it) },
            childFragmentManager
        )
        //项目统一 tab 字号，别用 ScaleTabLayout 默认的 16/18（在收藏页太抢眼）
        binding.tabLayout.normalTextSize = 13f
        binding.tabLayout.selectTextSize = 14f
        binding.tabLayout.setViewPager(binding.vpCollect, titles)
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        if (v == binding.ivBack) pop()
    }
}

package com.fingertip.uilib.fragment.worshiping

import android.view.View
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentLampBinding
import com.fingertip.uilib.fragment.shop.ShopFragment
import com.fingertip.uilib.utils.findMainFragment
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.MainVM

/**
 * 供灯（静态骨架）
 *
 * 方案三把供灯并进拜佛 tab 的 3D 佛龛场景，本页作为二级页占位：
 * 场景区、续燃卡、装扮格都按设计稿摆好位置。灯油已不在商城，
 * 续燃/灯油格回拜佛 tab 点佛灯操作；灯具/佛龛格可直达商城对应分类。
 *
 * 页面没有业务请求，initVM 只是满足基类，不发任何网络调用。
 */
class LampFragment : TopPmFragment<MainVM>() {

    override fun layoutId() = R.layout.fragment_lamp

    override fun initVM(): MainVM = provideVM()

    private val binding get() = mBinding as FragmentLampBinding

    override fun initShiTu() {
        //静态骨架，续燃状态后续接 lamp/status
    }

    override fun getClickViews() = listOf(
        binding.ivBack,
        binding.tvRelight,
        binding.llDress1,
        binding.llDress2,
        binding.llDress3
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            //灯油已移出商城：续燃/灯油格统一回拜佛页点佛灯操作
            binding.tvRelight, binding.llDress3 -> findMainFragment()?.selectTab(2)
            //装扮两格直挂商城对应分类：灯具外观 / 佛龛场景
            binding.llDress1 -> openPage(ShopFragment.newInstance(ShopDressConfig.TYPE_LAMP))
            binding.llDress2 -> openPage(ShopFragment.newInstance(ShopDressConfig.TYPE_SHRINE))
        }
    }
}

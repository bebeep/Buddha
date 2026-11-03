package com.fingertip.uilib.fragment.worshiping

import android.view.View
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentLampBinding
import com.fingertip.uilib.fragment.shop.ShopFragment
import com.fingertip.uilib.viewmodel.MainVM

/**
 * 供灯（静态骨架）
 *
 * 方案三把供灯并进拜佛 tab 的 3D 佛龛场景，本页先作为二级页占位：
 * 场景区、续燃卡、装扮三格都按设计稿摆好位置，续燃/装扮点击统一提示「即将上线」，
 * 等 IAP（Google Play Billing）与 lampConfig 就绪后，把点击换成真实埋点与购买流程。
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
            binding.tvRelight -> ToastUtil.showMessage(getString(R.string.lamp_relight_soon))
            //装扮三格直挂商城对应分类：灯具外观 / 佛龛场景 / 灯油
            binding.llDress1 -> openShop(ShopDressConfig.TYPE_LAMP)
            binding.llDress2 -> openShop(ShopDressConfig.TYPE_SHRINE)
            binding.llDress3 -> openShop(ShopDressConfig.TYPE_OIL)
        }
    }

    private fun openShop(dressType: Int) {
        (parentFragment as? com.fingertip.uilib.fragment.MainFragment)
            ?.start(ShopFragment.newInstance(dressType))
    }
}

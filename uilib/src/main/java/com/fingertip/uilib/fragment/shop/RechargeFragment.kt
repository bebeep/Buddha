package com.fingertip.uilib.fragment.shop

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.CoinPackData
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CoinPackAdapter
import com.fingertip.uilib.databinding.FragmentRechargeBinding
import com.fingertip.uilib.pay.IapBridge
import com.fingertip.uilib.viewmodel.RechargeVM

/**
 * 功德币充值
 *
 * 功德币是全站唯一货币（商品购买、供奉、灯油、祈福封面都扣它），
 * 入金只有这一条路：Google Play / AppStore 内购 → 服务端验单 → 加币。
 * 档位与比例来自后台 wx_pay_config（当前 1 元 = 10 功德币），客户端不写死。
 */
class RechargeFragment : TopPmFragment<RechargeVM>() {

    companion object {
        private const val TAG = "RechargeFragment"
        fun newInstance(): RechargeFragment = RechargeFragment()
    }

    override fun layoutId(): Int = R.layout.fragment_recharge

    override fun initVM(): RechargeVM = provideVM()

    private val binding get() = mBinding as FragmentRechargeBinding

    private lateinit var adapter: CoinPackAdapter

    //等待支付回执的档位与订单号
    private var pendingPack: CoinPackData? = null
    private var pendingOrderNo: String? = null

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        if (v == binding.ivBack) pop()
    }

    override fun initShiTu() {
        binding.tvBalance.text = "${GlobalConfig.userData?.coin ?: 0}"
        adapter = CoinPackAdapter(requireContext()) { onPackClick(it) }
        binding.rcPacks.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rcPacks.adapter = adapter
        binding.rcPacks.itemAnimator = null
        mViewModel.loadConfig()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.configResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                val list = result.data.orEmpty()
                adapter.initData(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            } else {
                log(TAG, "档位失败：${result.errorCode} ${result.errorMsg}")
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }

        mViewModel.orderResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val pack = pendingPack
            if (result.success && pack != null && !result.data.isNullOrBlank()) {
                pendingOrderNo = result.data
                launchPay(pack, result.data!!)
            } else {
                pendingPack = null
                pendingOrderNo = null
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.recharge_fail))
            }
        }

        mViewModel.verifyResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            pendingPack = null
            pendingOrderNo = null
            if (result.success) {
                //服务端返回到账后的余额，直接回写全局 + 界面
                result.data?.toIntOrNull()?.let { coin ->
                    GlobalConfig.userData?.coin = coin
                    binding.tvBalance.text = "$coin"
                }
                ToastUtil.showMessage(getString(R.string.recharge_success))
            } else {
                log(TAG, "充值验单失败：${result.errorCode} ${result.errorMsg} orderNo=$pendingOrderNo")
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.recharge_verify_fail))
            }
        }
    }

    private fun onPackClick(pack: CoinPackData) {
        if (GlobalConfig.session.isBlank()) {
            ToastUtil.showMessage(getString(R.string.shop_login_first))
            return
        }
        pendingPack = pack
        mViewModel.createOrder(pack.id)
    }

    private fun launchPay(pack: CoinPackData, orderNo: String) {
        //功德币是消耗型商品，买完要 consume，否则同一档位第二次买不了
        IapBridge.launchPurchase(
            activity = activity,
            productId = pack.androidProductId(),
            accountId = GlobalConfig.userData?.accountId ?: 0,
            consumable = true
        ) { result ->
            if (!isAdded) return@launchPurchase
            val payOrderId = result.payOrderId
            if (result.success && !payOrderId.isNullOrBlank()) {
                mViewModel.verify(orderNo, payOrderId)
            } else {
                //用户取消或通道未接入：订单停在「已创建」，不会加币，不存在资损
                log(TAG, "支付未完成：${result.message} orderNo=$orderNo")
                pendingPack = null
                pendingOrderNo = null
                ToastUtil.showMessage(result.message ?: getString(R.string.recharge_fail))
            }
        }
    }
}

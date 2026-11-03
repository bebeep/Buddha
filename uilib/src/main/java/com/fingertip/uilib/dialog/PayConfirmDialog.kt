package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.top.TopDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogPayConfirmBinding

/**
 * 购买确认弹窗。
 *
 * 只负责「让用户确认买什么、多少钱」，确认后由页面走
 * 建单 → 拉起商店支付 → 验单 的完整链路，弹窗本身不碰网络。
 */
class PayConfirmDialog(
    context: Context,
    private val item: ShopDressConfig,
    private val onConfirm: () -> Unit
) : TopDialog(context) {

    private lateinit var binding: DialogPayConfirmBinding

    override fun attachLayoutRes(): Int = R.layout.dialog_pay_confirm

    override fun onViewInit(view: View?) {
        if (view == null) return
        binding = DialogPayConfirmBinding.bind(view)

        binding.tvItem.text = item.dressName
        binding.tvPrice.text = "${item.priceText()} · ${item.durationText()}"

        //灯油是消耗品，提示一次一买，避免用户以为是永久买断
        val hint = if (item.isOil()) {
            context.getString(R.string.shop_pay_hint_oil)
        } else {
            context.getString(R.string.shop_pay_hint)
        }
        binding.tvHint.text = if (com.fingertip.uilib.pay.IapBridge.isMockMode()) {
            //开发环境必须说清楚，不然真机测试的人会以为被扣款了
            hint + "\n" + context.getString(R.string.shop_pay_mock_hint)
        } else {
            hint
        }

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            dismiss()
            onConfirm.invoke()
        }
    }
}

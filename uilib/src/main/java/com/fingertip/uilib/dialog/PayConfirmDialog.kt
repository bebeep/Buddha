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
 * 只负责「让用户确认买什么、花多少功德币」，确认后由页面调
 * dress/buyByCoin 扣币发货，弹窗本身不碰网络。
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
        val base = if (item.isOil()) {
            context.getString(R.string.shop_pay_hint_oil)
        } else {
            context.getString(R.string.shop_pay_hint)
        }
        //显示当前余额：功德币不足时不用等服务端报错，用户自己就能看出来
        val coin = com.fingertip.baselib.constant.GlobalConfig.userData?.coin ?: 0
        binding.tvHint.text = "$base\n我的功德币：$coin"

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            dismiss()
            onConfirm.invoke()
        }
    }
}

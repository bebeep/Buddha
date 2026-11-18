package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialog
import com.fingertip.uilib.databinding.DialogNianfoRewardBinding

/**
 * 念佛一藏圆满奖励弹窗（强提示）：
 * 标题「一藏圆满」+ 大号发光「修行值 +N」+ 说明行 + 继续按钮。
 *
 * @param exp      本次获得的修行值（0 = 当日已达上限）
 * @param roundNo  当天第几轮（1 起）
 * @param desc     说明行文案（外部拼好，含上限/奖励规则语境）
 */
class NianfoRewardDialog(
    context: Context,
    private val exp: Int,
    private val roundNo: Int,
    private val desc: String
) : TopDialog(context) {

    private lateinit var binding: DialogNianfoRewardBinding

    override fun attachLayoutRes(): Int = com.fingertip.uilib.R.layout.dialog_nianfo_reward

    override fun onViewInit(view: View?) {
        if (view == null) return
        binding = DialogNianfoRewardBinding.bind(view)
        val cn = cnNumeral(roundNo)
        binding.tvRewardTitle.text = "第${cn}藏 · 圆满"
        binding.tvRewardValue.text = if (exp > 0) "修行值 +$exp" else "修行值 +0"
        binding.tvRewardDesc.text = desc
        binding.tvRewardOk.setOnClickListener { dismiss() }
    }

    private fun cnNumeral(n: Int): String {
        if (n !in 1..99) return n.toString()
        val digits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        val tens = n / 10
        val ones = n % 10
        return when {
            n < 10 -> digits[n]
            tens == 1 -> if (ones == 0) "十" else "十" + digits[ones]
            else -> digits[tens] + "十" + if (ones == 0) "" else digits[ones]
        }
    }
}

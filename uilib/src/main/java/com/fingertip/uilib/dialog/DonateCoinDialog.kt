package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogDonateCoinBinding

/**
 * 功德捐赠弹窗：快捷金额/自定义，确认后交给页面 VM
 */
class DonateCoinDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    var onDonate: ((coins: Int) -> Unit)? = null

    private lateinit var binding: DialogDonateCoinBinding

    override fun getLayoutId(): Int = R.layout.dialog_donate_coin

    override fun onViewCreate(view: View) {
        binding = DialogDonateCoinBinding.bind(view)

        val quickViews = listOf(binding.tvAmount10, binding.tvAmount66, binding.tvAmount888)
        quickViews.forEach { tv ->
            tv.setOnClickListener {
                quickViews.forEach { it.setBackgroundResource(R.drawable.bg_praying_cover_normal) }
                tv.setBackgroundResource(R.drawable.bg_praying_cover_selected)
                binding.etAmount.setText(tv.text.toString())
            }
        }

        binding.tvConfirm.setOnClickListener {
            val coins = binding.etAmount.text?.toString()?.toIntOrNull() ?: 0
            if (coins <= 0) {
                ToastUtil.showMessage("请输入捐赠的功德币数")
                return@setOnClickListener
            }
            binding.tvConfirm.alpha = 0.6f
            onDonate?.invoke(coins)
        }
    }

    fun render(buddhaName: String?) {
        ensureViewCreated()
        binding.tvBuddhaName.text = "为「${buddhaName ?: "佛像"}」添功德"
    }

    fun onDonateSuccess() {
        dismiss()
    }

    fun onDonateFailed() {
        binding.tvConfirm.alpha = 1f
    }
}

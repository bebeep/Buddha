package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import android.widget.Toast
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogDeedDonateBinding

/**
 * 行善捐赠弹窗：快捷金额/自定义 + 留言，确认后交给页面 VM
 */
class DeedDonateDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    var onDonate: ((coins: Int, message: String?) -> Unit)? = null

    private lateinit var binding: DialogDeedDonateBinding

    override fun getLayoutId(): Int = R.layout.dialog_deed_donate

    override fun onViewCreate(view: View) {
        binding = DialogDeedDonateBinding.bind(view)

        val quickViews = listOf(binding.tvAmount5, binding.tvAmount10, binding.tvAmount88)
        quickViews.forEach { tv ->
            tv.setOnClickListener {
                quickViews.forEach {
                    it.setBackgroundResource(R.drawable.bg_praying_cover_normal)
                }
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
            val message = binding.etMessage.text?.toString()?.trim()
            binding.tvConfirm.alpha = 0.6f
            onDonate?.invoke(coins, message?.takeIf { it.isNotEmpty() })
        }
    }

    fun render(title: String?) {
        ensureViewCreated()
        binding.tvDeedTitle.text = title ?: ""
    }

    fun onDonateSuccess() {
        dismiss()
    }

    fun onDonateFailed() {
        binding.tvConfirm.alpha = 1f
    }
}

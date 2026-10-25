package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogPrayingRuleBinding

/**
 * 祈福规则弹窗
 */
class PrayingRuleDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    private lateinit var binding: DialogPrayingRuleBinding

    override fun getLayoutId(): Int = R.layout.dialog_praying_rule

    override fun onViewCreate(view: View) {
        binding = DialogPrayingRuleBinding.bind(view)
        binding.tvClose.setOnClickListener { dismiss() }
    }
}

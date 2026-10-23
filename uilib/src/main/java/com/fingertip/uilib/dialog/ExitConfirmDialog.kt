package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogExitConfirmBinding

/**
 * 退出应用确认弹窗
 *
 * 首页(HostFragment)按返回时才弹，其余 tab 按返回是切回首页。
 * 只负责展示和回调，退不退由外面决定。
 */
class ExitConfirmDialog(
    context: Context,
    private val onExit: () -> Unit
) : TopDialog(context) {

    private lateinit var binding: DialogExitConfirmBinding

    override fun attachLayoutRes(): Int = R.layout.dialog_exit_confirm

    override fun onViewInit(view: View?) {
        if (view == null) return
        binding = DialogExitConfirmBinding.bind(view)
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnExit.setOnClickListener {
            dismiss()
            onExit.invoke()
        }
    }
}

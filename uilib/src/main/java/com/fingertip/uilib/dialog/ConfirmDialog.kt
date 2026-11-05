package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogExitConfirmBinding

/**
 * 通用二次确认弹窗
 *
 * 复用「退出应用」那套版式（280dp 白卡 + 双按钮），文案由调用方给。
 * 用在退出登录、注销账号这类不可逆操作前——先去写一套新的弹窗版式没有意义，
 * 长得一样用户才认得出"这是要确认一下"。
 */
class ConfirmDialog(
    context: Context,
    private val title: String,
    private val message: String,
    private val confirmText: String = "确定",
    private val cancelText: String = "取消",
    /** 点取消时的回调；为空表示取消即仅关闭弹窗 */
    private val onCancel: (() -> Unit)? = null,
    private val onConfirm: () -> Unit
) : TopDialog(context) {

    private lateinit var binding: DialogExitConfirmBinding

    override fun attachLayoutRes(): Int = R.layout.dialog_exit_confirm

    override fun onViewInit(view: View?) {
        if (view == null) return
        binding = DialogExitConfirmBinding.bind(view)
        binding.tvTitle.text = title
        binding.tvContent.text = message
        binding.btnExit.text = confirmText
        binding.btnCancel.text = cancelText
        binding.btnCancel.setOnClickListener {
            dismiss()
            onCancel?.invoke()
        }
        binding.btnExit.setOnClickListener {
            dismiss()
            onConfirm.invoke()
        }
    }
}

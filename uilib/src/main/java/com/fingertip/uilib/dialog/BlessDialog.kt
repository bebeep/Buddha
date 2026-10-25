package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.WishRecordBean
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogBlessBinding

/**
 * 为愿望送祝福底部弹窗，对应原型「查看愿望并为他人祝愿」
 * 祝福内容必填（服务端校验），成功后回调外部刷新
 */
class BlessDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    //点「祝愿」，请求交给外部
    var onSubmit: ((wishingId: Int, content: String) -> Unit)? = null

    private lateinit var binding: DialogBlessBinding
    private var wishRecord: WishRecordBean? = null

    override fun getLayoutId(): Int = R.layout.dialog_bless

    override fun onViewCreate(view: View) {
        binding = DialogBlessBinding.bind(view)
        binding.tvBlessSubmit.setOnClickListener {
            val content = binding.etBlessContent.text?.toString()?.trim().orEmpty()
            val wish = wishRecord ?: return@setOnClickListener
            if (content.isEmpty()) {
                ToastUtil.showMessage("祝福内容不能为空")
                return@setOnClickListener
            }
            binding.tvBlessSubmit.alpha = 0.6f
            onSubmit?.invoke(wish.id, content)
        }
    }

    /**
     * 填充愿望内容
     */
    fun render(wishRecord: WishRecordBean?) {
        this.wishRecord = wishRecord
        binding.tvWishContent.text = wishRecord?.wishContent.orEmpty()
    }

    /**
     * 祝福成功
     */
    fun onBlessSuccess() {
        ToastUtil.showMessage("祝愿成功，功德无量")
        dismiss()
    }

    /**
     * 祝福失败（含重复祝愿），提示服务端文案并恢复按钮
     */
    fun onBlessFailed(msg: String?) {
        binding.tvBlessSubmit.alpha = 1f
        ToastUtil.showMessage(msg ?: "祝愿失败，请稍后再试")
    }
}

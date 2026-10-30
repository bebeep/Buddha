package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogWishRuleBinding

/**
 * 许愿规则弹窗，对应原型「许愿规则」
 * 文案由外部传入，没有就用默认
 */
class WishRuleDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    companion object {
        private const val DEFAULT_RULE =
            "1、每个用户每天限许愿1次，当日24:00重置；\n2、每个用户每天限祝愿5次，当日24:00重置；\n3、许愿成功增加修行值，收到祝福也有修行值奖励；\n4、愿望内容经审核后展示，请文明许愿。"
    }

    private lateinit var binding: DialogWishRuleBinding

    override fun getLayoutId(): Int = R.layout.dialog_wish_rule

    override fun onViewCreate(view: View) {
        binding = DialogWishRuleBinding.bind(view)
        binding.tvRuleOk.setOnClickListener { dismiss() }
    }

    fun render(rule: String?) {
        ensureViewCreated()
        binding.tvRuleContent.text = rule?.takeIf { it.isNotBlank() } ?: DEFAULT_RULE
    }
}

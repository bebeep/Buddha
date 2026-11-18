package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogWishRuleBinding

/**
 * 通用规则弹窗（许愿规则同款版式）：底部弹窗 + 标题 + 正文 + 「我已明白」。
 * 标题/正文由外部传入，不传用许愿默认文案——念佛等页面的规则弹窗都走这里。
 */
class WishRuleDialog(
    context: Context,
    private val title: String? = null,
    private val rule: String? = null
) : TopDialogBottomSheetDialog(context) {

    companion object {
        private const val DEFAULT_TITLE = "许愿规则"
        private const val DEFAULT_RULE =
            "1、每个用户每天限许愿1次，当日24:00重置；\n2、每个用户每天限祝愿5次，当日24:00重置；\n3、许愿成功增加修行值，收到祝福也有修行值奖励；\n4、愿望内容经审核后展示，请文明许愿。"
    }

    private lateinit var binding: DialogWishRuleBinding

    override fun getLayoutId(): Int = R.layout.dialog_wish_rule

    override fun onViewCreate(view: View) {
        binding = DialogWishRuleBinding.bind(view)
        binding.tvRuleTitle.text = title?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE
        binding.tvRuleOk.setOnClickListener { dismiss() }
        binding.tvRuleContent.text = rule?.takeIf { it.isNotBlank() } ?: DEFAULT_RULE
    }
}

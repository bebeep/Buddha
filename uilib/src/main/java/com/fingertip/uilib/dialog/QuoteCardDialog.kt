package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogQuoteCardBinding
import com.fingertip.uilib.utils.DailyQuotes

/**
 * 禅语日签详情。首页「佛友圈精选」里补位卡的点击承接。
 *
 * 补位卡是本地 DailyQuotes 的经藏原文，没有 momentId，进不了动态详情页，
 * 以前点了会切到佛友圈列表，看起来像"点错了"。这里给一张完整的日签卡：
 * 原文 + 出处一眼看完，想找人聊再去佛友圈。
 */
class QuoteCardDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    /** 底部「去佛友圈看看」，由调用方切 tab */
    var onGoMoment: (() -> Unit)? = null

    private lateinit var binding: DialogQuoteCardBinding

    override fun getLayoutId(): Int = R.layout.dialog_quote_card

    override fun onViewCreate(view: View) {
        binding = DialogQuoteCardBinding.bind(view)
        binding.tvGoMoment.setOnClickListener {
            // 先关弹窗再切 tab，避免 sheet 盖在新页面上
            dismiss()
            onGoMoment?.invoke()
        }
    }

    fun render(quote: DailyQuotes.Quote) {
        ensureViewCreated()
        binding.tvContent.text = quote.text
        binding.tvSource.text = quote.source
    }
}

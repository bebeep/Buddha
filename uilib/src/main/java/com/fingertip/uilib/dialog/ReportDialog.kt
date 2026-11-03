package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogReportBinding

/**
 * 举报弹窗
 *
 * Apple 审核指南 1.2 与 Google UGC 政策都要求 UGC 产品具备举报机制，
 * 且举报项要覆盖实际违规类型。这里的五项是平台最常见的判罚口径：
 * 垃圾广告 / 色情低俗 / 辱骂骚扰 / 宗教诈骗 / 其他。
 *
 * 「宗教诈骗」是这类产品的重点：冒充法师收款、假借开光收费，
 * 在东南亚市场是高发问题，单独列一项便于后台按 tag 快速处置。
 */
class ReportDialog(
    context: Context,
    private val onSubmit: (tag: String, content: String) -> Unit
) : TopDialog(context) {

    companion object {
        //与后端 report_record.reportTag 对应，后台按这些值做分类统计
        const val TAG_SPAM = "spam"
        const val TAG_PORN = "porn"
        const val TAG_ABUSE = "abuse"
        const val TAG_FRAUD = "fraud"
        const val TAG_OTHER = "other"

        private val TAGS = arrayOf(TAG_SPAM, TAG_PORN, TAG_ABUSE, TAG_FRAUD, TAG_OTHER)
    }

    private lateinit var binding: DialogReportBinding

    //0 = 未选，1..5 对应 TAGS 下标
    private var selected = 0

    override fun attachLayoutRes(): Int = R.layout.dialog_report

    override fun onViewInit(view: View?) {
        if (view == null) return
        binding = DialogReportBinding.bind(view)

        val reasons = listOf(
            binding.tvReason1, binding.tvReason2, binding.tvReason3,
            binding.tvReason4, binding.tvReason5
        )
        reasons.forEachIndexed { index, tv ->
            tv.setOnClickListener { select(index + 1) }
        }

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSubmit.setOnClickListener {
            if (selected == 0) {
                ToastUtil.showMessage(context.getString(R.string.report_pick_reason))
                return@setOnClickListener
            }
            dismiss()
            onSubmit.invoke(TAGS[selected - 1], "")
        }
    }

    private fun select(index: Int) {
        selected = index
        val reasons = listOf(
            binding.tvReason1, binding.tvReason2, binding.tvReason3,
            binding.tvReason4, binding.tvReason5
        )
        reasons.forEachIndexed { i, tv ->
            val on = i + 1 == index
            tv.setBackgroundResource(if (on) R.drawable.bg_report_item_on else 0)
            tv.setTextColor(if (on) 0xFF8A6A2F.toInt() else 0xFF2C1900.toInt())
        }
    }
}

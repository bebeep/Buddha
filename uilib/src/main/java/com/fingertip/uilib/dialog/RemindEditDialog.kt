package com.fingertip.uilib.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.fingertip.baselib.top.TopDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.view.MyTextView
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogRemindEditBinding
import java.util.Locale

/**
 * 提醒规则编辑弹窗
 *
 * 同一个弹窗干两件事：
 * - 自定义提醒：填文案 + 选时刻
 * - 节日/斋日：只选时刻（文案框隐藏，GONE 之后下面的格子会自动贴上去）
 *
 * 时刻只给 06:00~20:00 这 15 个整点。提醒是为了修行，凌晨三点的提醒只会被关掉；
 * 整点粒度也够用，分钟级选择在这个场景里纯属给用户添麻烦。
 */
class RemindEditDialog(
    context: Context,
    private val title: String,
    private val showTextInput: Boolean,
    private val initialText: String = "",
    initialHour: Int = DEFAULT_HOUR,
    private val onConfirm: (text: String, hour: Int) -> Unit
) : TopDialog(context) {

    private companion object {
        /** 可选时段：早 6 点到晚 8 点 */
        val HOURS = (6..20).toList()

        /** 默认早 8 点 */
        const val DEFAULT_HOUR = 8

        /** 一行几个格子 */
        const val COLUMNS = 5

        /** 选中态：深色字压在金色底上 */
        const val CHIP_TEXT_ON = 0xFF1C1611.toInt()

        /** 未选中：米色字 */
        const val CHIP_TEXT_OFF = 0xFFB8AC96.toInt()
    }

    private lateinit var binding: DialogRemindEditBinding

    /** 当前选中的小时 */
    private var selectedHour = initialHour.coerceIn(HOURS.first(), HOURS.last())

    /** 所有可选格子，切换选中态时要遍历 */
    private val chips = ArrayList<MyTextView>()

    override fun attachLayoutRes(): Int = R.layout.dialog_remind_edit

    override fun onViewInit(view: View?) {
        if (view == null) return
        binding = DialogRemindEditBinding.bind(view)

        binding.tvTitle.text = title

        binding.etText.visibility = if (showTextInput) View.VISIBLE else View.GONE
        if (showTextInput) {
            binding.etText.setText(initialText)
            binding.etText.setSelection(initialText.length)
        }

        buildHourChips()

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnConfirm.setOnClickListener {
            val text = if (showTextInput) binding.etText.text?.toString()?.trim().orEmpty() else ""
            if (showTextInput && text.isEmpty()) {
                ToastUtil.showMessage(context.getString(R.string.remind_dialog_text_empty))
                return@setOnClickListener
            }
            dismiss()
            onConfirm(text, selectedHour)
        }
    }

    /**
     * 15 个整点按 5 列铺开。格子用 item_remind_hour 反复 inflate——
     * 每次 new MyTextView 都要重新 load 一次字体，反而更慢。
     */
    private fun buildHourChips() {
        val inflater = LayoutInflater.from(context)
        val density = context.resources.displayMetrics.density
        var row: LinearLayout? = null
        chips.clear()

        HOURS.forEachIndexed { index, hour ->
            if (index % COLUMNS == 0) {
                row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = (8 * density).toInt() }
                }
                binding.llHours.addView(row)
            }
            // 根节点本身就是 MyTextView，直接 cast，不用 findViewById
            val chip = inflater.inflate(R.layout.item_remind_hour, binding.llHours, false) as MyTextView
            chip.text = String.format(Locale.US, "%02d:00", hour)
            chip.setOnClickListener { select(hour) }
            row?.addView(chip)
            chips.add(chip)
        }
        // 初始选中态最后统一刷一遍，省得每个格子写两份判断
        select(selectedHour)
    }

    private fun select(hour: Int) {
        selectedHour = hour
        chips.forEachIndexed { index, chip ->
            val selected = HOURS[index] == hour
            chip.isSelected = selected
            chip.setTextColor(if (selected) CHIP_TEXT_ON else CHIP_TEXT_OFF)
        }
    }
}

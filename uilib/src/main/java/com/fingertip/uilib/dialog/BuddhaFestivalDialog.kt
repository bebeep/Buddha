package com.fingertip.uilib.dialog

import android.content.Context
import android.graphics.Color
import android.view.View
import com.fingertip.baselib.bean.BuddhaFestival
import com.fingertip.baselib.top.TopDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogBuddhaFestivalBinding

/**
 * 佛历节日简介弹窗
 *
 * 点日历下方「本月佛历节日」列表里的某一条时弹出，内容直接取月历接口带回的字段，
 * 不再单独请求详情接口。
 */
class BuddhaFestivalDialog(
    context: Context,
    private val festival: BuddhaFestival
) : TopDialog(context) {

    companion object {
        //佛菩萨圣诞/成道/出家/涅槃
        private const val COLOR_MAJOR = "#D9432E"
        //盂兰盆、卫塞节等佛教节日
        private const val COLOR_MINOR = "#C8860D"
        //传统节日
        private const val COLOR_TRADITION = "#5A4632"
    }

    private lateinit var binding: DialogBuddhaFestivalBinding

    override fun attachLayoutRes(): Int = R.layout.dialog_buddha_festival

    override fun onViewInit(view: View?) {
        if (view == null) return
        binding = DialogBuddhaFestivalBinding.bind(view)

        val color = when (festival.level) {
            BuddhaFestival.LEVEL_BUDDHA -> COLOR_MAJOR
            BuddhaFestival.LEVEL_FESTIVAL -> COLOR_MINOR
            else -> COLOR_TRADITION
        }

        binding.tvTitle.text = festival.name.orEmpty()
        binding.tvTitle.setTextColor(Color.parseColor(color))

        binding.tvTag.setText(
            when {
                festival.today -> "今日"
                festival.level == BuddhaFestival.LEVEL_TRADITION -> "传统节日"
                else -> "佛历节日"
            }
        )
        binding.tvTag.setBackgroundResource(
            if (festival.level == BuddhaFestival.LEVEL_FESTIVAL) {
                R.drawable.bg_festival_tag_minor
            } else {
                R.drawable.bg_festival_tag_major
            }
        )

        val meta = listOfNotNull(
            festival.lunarDate?.takeIf { it.isNotBlank() }?.let { "农历$it" },
            festival.dateLabel().takeIf { it.isNotBlank() },
            festival.countdownLabel().takeIf { !festival.today && festival.daysFromToday > 0 }
        )
        binding.tvMeta.text = meta.joinToString("  ·  ")

        val desc = festival.description?.takeIf { it.isNotBlank() }
        binding.vDivider.visibility = if (desc == null) View.GONE else View.VISIBLE
        binding.tvDesc.visibility = if (desc == null) View.GONE else View.VISIBLE
        binding.tvDesc.text = desc.orEmpty()

        val buddhaName = festival.buddhaName?.takeIf { it.isNotBlank() }
        binding.tvBuddha.visibility = if (buddhaName == null) View.GONE else View.VISIBLE
        binding.tvBuddha.text = "关联佛像：$buddhaName"

        binding.btnConfirm.setOnClickListener { dismiss() }
    }
}

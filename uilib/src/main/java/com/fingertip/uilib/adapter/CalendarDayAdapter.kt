package com.fingertip.uilib.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import com.fingertip.baselib.bean.BuddhaFestival
import com.fingertip.baselib.bean.CalendarSimpleInfo
import com.fingertip.baselib.bean.CalendarSimpleRsp
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemCalendarDayBinding

/**
 * 月历格子 Adapter
 *
 * 服务端把上/下月补位日期也放在列表里，格子背景按状态区分：
 * - 今天：实心橙圆
 * - 已签到：淡橙圆
 * - 非本月：淡化
 *
 * 格子副标题优先级：佛历节日短名 > 农历日。佛菩萨圣诞用朱红、佛教节日用琥珀、
 * 传统节日（春节中秋等）用灰色，让节日在一屏日历里一眼能挑出来。
 */
class CalendarDayAdapter(context: Context) : TopRcAdapter<CalendarSimpleInfo, TopRcAdapter.TopRcViewHolder>(context) {

    companion object {
        //佛菩萨圣诞/成道/出家/涅槃
        private const val COLOR_FESTIVAL_MAJOR = 0xFFD9432E.toInt()
        //盂兰盆、卫塞节等佛教节日
        private const val COLOR_FESTIVAL_MINOR = 0xFFC8860D.toInt()
        //春节、中秋等传统节日
        private const val COLOR_FESTIVAL_TRADITION = 0xFF8A8A8A.toInt()
    }

    override fun initLayoutId(viewType: Int): Int = R.layout.item_calendar_day

    //当前展示的年月，用于判断补位日期
    private var year: Int = 0
    private var month: Int = 0

    //整包响应，主要用 nowDate 判断今天
    private var rsp: CalendarSimpleRsp? = null

    fun setData(year: Int, month: Int, rsp: CalendarSimpleRsp?) {
        this.year = year
        this.month = month
        this.rsp = rsp
        initData(rsp?.calendarList)
    }

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemCalendarDayBinding>()
        val item = get(position) ?: return

        val inMonth = item.inMonth(year, month)
        val isToday = item.isToday(rsp)

        binding.tvSolar.text = item.solarDay()
        binding.tvSolar.setBackgroundResource(
            when {
                isToday -> R.drawable.bg_sign_calendar_today
                item.hasSignIn -> R.drawable.bg_sign_calendar_signed
                else -> 0
            }
        )
        //今天白字压在橙圆上，其余深色
        binding.tvSolar.setTextColor(if (isToday) Color.WHITE else Color.parseColor("#2C1900"))

        //副标题：节日优先，其次农历；颜色服务端可指定（如节假日红字），格式「0xffffff」
        binding.tvLunar.text = item.subtitle()
        val festivalColor = festivalColor(item.festivalLevel)
        when {
            festivalColor != null -> binding.tvLunar.setTextColor(festivalColor)
            !item.lunarDateTextColor.isNullOrBlank() -> binding.tvLunar.setTextColor(parseServerColor(item.lunarDateTextColor!!))
            else -> binding.tvLunar.setTextColor(Color.parseColor(if (inMonth) "#999999" else "#CCCCCC"))
        }
        //1、2 级节日加粗，在一屏小字里更醒目
        binding.tvLunar.typeface = if (item.festivalLevel in 1..2) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        //非本月补位日期整体淡化
        holder.itemView.alpha = if (inMonth) 1f else 0.35f
    }

    /**
     * 节日级别对应的字色，传统节日用灰，无节日返回 null 走默认逻辑
     */
    private fun festivalColor(level: Int): Int? = when (level) {
        BuddhaFestival.LEVEL_BUDDHA -> COLOR_FESTIVAL_MAJOR
        BuddhaFestival.LEVEL_FESTIVAL -> COLOR_FESTIVAL_MINOR
        BuddhaFestival.LEVEL_TRADITION -> COLOR_FESTIVAL_TRADITION
        else -> null
    }

    /**
     * 服务端颜色是「0xffffff」格式，转成 Android 认的 #ffffff
     */
    private fun parseServerColor(raw: String): Int = runCatching {
        Color.parseColor(raw.replaceFirst("0x", "#", ignoreCase = true))
    }.getOrDefault(Color.parseColor("#999999"))
}

package com.fingertip.uilib.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
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
 */
class CalendarDayAdapter(context: Context) : TopRcAdapter<CalendarSimpleInfo, TopRcAdapter.TopRcViewHolder>(context) {

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

        //农历/节气，颜色服务端可指定（如节假日红字），格式「0xffffff」
        binding.tvLunar.text = item.lunarDate.orEmpty()
        val lunarColor = item.lunarDateTextColor
        if (!lunarColor.isNullOrBlank()) {
            binding.tvLunar.setTextColor(parseServerColor(lunarColor))
        } else {
            binding.tvLunar.setTextColor(Color.parseColor(if (inMonth) "#999999" else "#CCCCCC"))
        }

        //非本月补位日期整体淡化
        holder.itemView.alpha = if (inMonth) 1f else 0.35f
    }

    /**
     * 服务端颜色是「0xffffff」格式，转成 Android 认的 #ffffff
     */
    private fun parseServerColor(raw: String): Int = runCatching {
        Color.parseColor(raw.replaceFirst("0x", "#", ignoreCase = true))
    }.getOrDefault(Color.parseColor("#999999"))
}

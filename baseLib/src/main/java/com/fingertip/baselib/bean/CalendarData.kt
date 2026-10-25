package com.fingertip.baselib.bean

/**
 * 月历接口数据，对应服务端 account/getMonthCalendar 返回的 CalendarSimpleRsp
 * （com.buddha.web.model.apiModel.CalendarSimpleRsp）
 */
class CalendarSimpleRsp {
    //整月日历格子（含上/下月补位的日期），顺序即展示顺序，7 个一行
    var calendarList: List<CalendarSimpleInfo>? = null
    //今日详情（农历、干支、宜忌等）
    var calendarInfo: CalendarInfo? = null
    //当前系统日期 yyyy-MM-dd
    var nowDate: String? = null
    //当前星期几
    var nowWeekDay: Int = 0
    //今天是否已签到
    var hasSignIn: Boolean = false
}

/**
 * 日历上的一个格子，对应服务端 CalendarSimpleInfo
 */
class CalendarSimpleInfo {
    //公历日期 yyyy-MM-dd
    var solarDate: String? = null
    //农历日，如「初一」「廿四」，节气/节日时是节气名，如「清明」「谷雨」
    var lunarDate: String? = null
    //农历日期字体颜色，如「0xffffff」，空则用默认色
    var lunarDateTextColor: String? = null
    //这天是否签到过
    var hasSignIn: Boolean = false
    //星期几，1~7 对应格子所在列（列头从左到右）
    var nowWeekDay: Int = 0

    //公历日数字，从 solarDate 解析出来，如「4」「16」
    fun solarDay(): String {
        val date = solarDate ?: return ""
        return date.substringAfterLast('-').toIntOrNull()?.toString() ?: ""
    }

    /**
     * 是否本月日期。上下月补位的日期也在这份列表里，
     * 拿 nowDate 的年月跟格子比对，不一致的就是补位日期，要淡化显示
     */
    fun inMonth(year: Int, month: Int): Boolean {
        val date = solarDate ?: return false
        val parts = date.split("-")
        if (parts.size < 2) return false
        return parts[0].toIntOrNull() == year && parts[1].toIntOrNull() == month
    }

    fun isToday(rsp: CalendarSimpleRsp?): Boolean = solarDate != null && solarDate == rsp?.nowDate
}

/**
 * 单日详情，对应服务端 CalendarInfo，月历接口会把今天的详情一并带回
 */
class CalendarInfo {
    //公历日期 yyyy-MM-dd
    var solarDate: String? = null
    //星期几
    var weekDay: Int = 0
    //农历完整描述，如「庚子年三月廿四」
    var lunarDate: String? = null
    //佛历年份（公历年份+543），如 2564
    var buddhistYear: Int = 0
    //生肖，如「鼠」
    var zodiac: String? = null
    //干支，如「乙丑」
    var ganZhi: String? = null
    //宜，如「祭祀，余事勿取」
    var suitable: String? = null
    //忌，如「造庙，嫁娶，安床，余事勿取」
    var avoid: String? = null
    //吉神宜趋
    var jishen: String? = null
    //凶神宜忌
    var xiongshen: String? = null
    //彭祖百忌
    var baiji: String? = null
    //冲煞
    var chongsha: String? = null
    //五行
    var wuxing: String? = null
}

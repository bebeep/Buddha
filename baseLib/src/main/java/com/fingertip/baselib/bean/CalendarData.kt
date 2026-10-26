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
    //本月佛历节日（佛菩萨圣诞/成道/出家/涅槃 + 佛教节日），按日期升序
    var festivals: List<BuddhaFestival>? = null
    //今天是什么节日，无则 null
    var todayFestival: BuddhaFestival? = null
    //下一个佛历节日（不含今天），用于倒计时
    var nextFestival: BuddhaFestival? = null
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

    //节日全名，如「释迦牟尼佛圣诞」，无节日为 null
    var festivalName: String? = null
    //节日短名，如「佛诞」，格子优先显示这个
    var festivalShortName: String? = null
    //节日级别 0无 1佛菩萨圣诞/成道/出家/涅槃 2佛教节日 3传统节日
    var festivalLevel: Int = 0
    //关联佛像id
    var festivalBuddhaId: Int? = null

    //格子要显示的副标题：节日优先，其次农历
    fun subtitle(): String =
        festivalShortName?.takeIf { it.isNotBlank() }
            ?: lunarDate.orEmpty()

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
 * 佛历节日，对应服务端 BuddhaFestival
 *
 * 服务端按农历月日算出整月的节日，佛菩萨圣诞/成道/出家/涅槃为 1 级，
 * 盂兰盆、卫塞节等佛教节日为 2 级，春节中秋等传统节日为 3 级（只在格子里显示）
 */
class BuddhaFestival {
    companion object {
        const val LEVEL_BUDDHA = 1
        const val LEVEL_FESTIVAL = 2
        const val LEVEL_TRADITION = 3
    }

    //公历日期 yyyy-MM-dd
    var solarDate: String? = null
    //农历日期，如「四月初八」
    var lunarDate: String? = null
    //节日全名，如「释迦牟尼佛圣诞」
    var name: String? = null
    //格子内短名，如「佛诞」
    var shortName: String? = null
    //级别，见 LEVEL_*
    var level: Int = 0
    //节日简介
    var description: String? = null
    //关联佛像id，有则可跳转佛像详情
    var buddhaId: Int? = null
    //关联佛像名
    var buddhaName: String? = null
    //是否就是今天
    var today: Boolean = false
    //距今天数：0 今天、正数未来、负数已过
    var daysFromToday: Int = 0

    //「5月24日 周日」这样的展示文案
    fun dateLabel(): String {
        val date = solarDate ?: return ""
        val parts = date.split("-")
        if (parts.size < 3) return date
        val month = parts[1].toIntOrNull() ?: return date
        val day = parts[2].toIntOrNull() ?: return date
        return "${month}月${day}日"
    }

    //距离今天的文案：今天 / 还有 N 天
    fun countdownLabel(): String = when {
        today || daysFromToday == 0 -> "今天"
        daysFromToday > 0 -> "还有${daysFromToday}天"
        else -> "已过"
    }
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

/**
 * 今日佛历信息，对应服务端 account/getTodayBuddhaInfo 返回的 TodayBuddhaInfo
 *
 * 首页顶部两行文案全靠它：dateText 是「佛历 2569 年  农历丙午年 九月初六」，
 * moodText 服务端已按 佛历节日 > 传统节日 > 节气 > 斋日 > 农历 逐级降级，永不为空
 */
class TodayBuddhaInfo {
    companion object {
        //moodText 命中的最高层级
        const val MOOD_BUDDHA = 1
        const val MOOD_TRADITION = 2
        const val MOOD_TERM = 3
        const val MOOD_ZHAI = 4
        const val MOOD_LUNAR = 5
    }

    var solarDate: String? = null
    //顶部主标题：佛历 2569 年  农历丙午年 九月初六
    var dateText: String? = null
    //副标题：观音菩萨出家日，六斋日
    var moodText: String? = null
    //moodText 命中的层级，见 MOOD_*
    var moodType: Int = 0

    var buddhistYear: Int = 0
    //农历月日，如「九月初六」
    var lunarDate: String? = null
    //农历全称，如「农历丙午年 九月初六」
    var lunarText: String? = null
    var ganZhi: String? = null
    var zodiac: String? = null
    //节气，非节气日为 null
    var term: String? = null

    //今天是否是佛历/传统节日
    var festival: BuddhaFestival? = null
    //是否斋日
    var zhaiDay: Boolean = false
    //六斋日 / 十斋日
    var zhaiDayName: String? = null
    //下一个佛历节日（不含今天）
    var nextFestival: BuddhaFestival? = null
}

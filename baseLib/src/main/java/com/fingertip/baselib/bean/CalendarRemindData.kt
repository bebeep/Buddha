package com.fingertip.baselib.bean

/**
 * 佛历订阅提醒规则，对应服务端 calendar_remind_config 表
 *
 * 一行 = 一条规则，三类：
 * 1 佛历节日（佛菩萨圣诞/成道/涅槃 + 盂兰盆、卫塞节等），可设提前天数
 * 2 斋日（农历初一、十五）
 * 3 每日自定义提醒（festivalKey 存用户填的文案）
 *
 * remindTime 存的是**用户本地时区**的时刻，不是服务器时间。
 * 服务端推送时用账号上报的 timeZone 反算，用户换时区不用改配置。
 */
class CalendarRemindData {

    companion object {
        const val TYPE_FESTIVAL = 1
        const val TYPE_ZHAI = 2
        const val TYPE_CUSTOM = 3

        /** 自定义提醒最多几条，与服务端 MAX_CUSTOM_REMIND 保持一致 */
        const val MAX_CUSTOM = 5

        /** 时刻解析不出来时的兜底小时 */
        private const val DEFAULT_HOUR = 8
    }

    /** 0 表示新建，>0 表示更新已有规则 */
    var id: Int = 0
    var remindType: Int = TYPE_FESTIVAL
    /** 自定义提醒的文案；节日/斋日为空 */
    var festivalKey: String? = null
    /** 提前天数：0 当天、-1 前一天、-2 前两天、-3 三天前 */
    var remindOffset: Int = 0
    /** 服务端原样返回的时间串 HH:mm:ss */
    var remindTime: String? = null
    /** 服务端拼好的 HH:mm，列表直接用，省得客户端解析 */
    var remindTimeText: String? = null
    /** 0 停用 1 启用 */
    var isEnabled: Int = 1

    fun enabled(): Boolean = isEnabled == 1

    /** 时刻整点，解析不出来时给 8 点兜底 */
    fun hour(): Int {
        val text = remindTimeText ?: remindTime ?: return DEFAULT_HOUR
        return text.substringBefore(':').toIntOrNull()?.coerceIn(0, 23) ?: DEFAULT_HOUR
    }

    /** 「07:00」 */
    fun timeLabel(): String = String.format(java.util.Locale.US, "%02d:00", hour())

    /** 提前天数的展示文案 */
    fun offsetLabel(): String = when {
        remindOffset >= 0 -> "当天"
        else -> "提前 ${-remindOffset} 天"
    }

    /** 自定义提醒在列表里的标题 */
    fun title(): String = when (remindType) {
        TYPE_FESTIVAL -> "佛历节日提醒"
        TYPE_ZHAI -> "斋日提醒"
        else -> festivalKey?.takeIf { it.isNotBlank() } ?: "自定义提醒"
    }

    /** 规则副标题，解释这条规则什么时候响 */
    fun subtitle(): String = when (remindType) {
        TYPE_FESTIVAL -> "佛菩萨圣诞、成道、涅槃等重要节日，${offsetLabel()}"
        TYPE_ZHAI -> "农历初一、十五，当天提醒"
        else -> "每天 ${timeLabel()} 提醒"
    }
}

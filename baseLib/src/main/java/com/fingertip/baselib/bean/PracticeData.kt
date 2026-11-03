package com.fingertip.baselib.bean

/**
 * 早课连续统计，对应服务端 practice/getStatistic 返回的 PracticeStatistic 实体。
 *
 * 连续天数、累计次数、已领里程碑全部以服务端为准——客户端只做展示，
 * 不参与判定，否则改天改本地就能刷里程碑奖励。
 */
class PracticeStatisticData {
    var id: Int = 0
    var accountId: Int = 0
    //当前连续天数
    var currentStreak: Int = 0
    //历史最长连续天数
    var maxStreak: Int = 0
    //累计完成次数
    var totalCount: Int = 0
    //最近一次修持日期 yyyy-MM-dd
    var lastPracticeDate: String? = null
    //已领取的里程碑，逗号分隔如 "7,21"
    var milestoneGain: String? = null
    var updateDate: String? = null

    companion object {
        //里程碑天数，与服务端 PracticeServiceImpl.MILESTONE_DAYS 保持一致
        val MILESTONE_DAYS = intArrayOf(7, 21, 49, 108)

        /**
         * 下一个里程碑天数。已过 108 天返回 0（一期没有更远的里程碑）
         */
        fun nextMilestone(streak: Int): Int {
            for (d in MILESTONE_DAYS) {
                if (streak < d) return d
            }
            return 0
        }
    }
}

/**
 * 供灯状态，对应服务端 lamp/getStatus 返回的 LampStatusData。
 *
 * 与首页灯条用的 [LampStatusData] 字段一致，但这里是独立接口返回的完整对象；
 * 首页那份是聚合接口内嵌的扁平字段，两者刻意分开，避免一端改字段影响另一端。
 */
class LampDetailData {
    //0无灯 1燃烧中 2将熄（剩余<=3小时）
    var lampStatus: Int = 0
    //剩余小时数，不足 1 小时按 0 计
    var remainHours: Int = 0
    //灯具外观名称，默认灯时为空
    var lampName: String? = null

    fun isNone() = lampStatus == 0

    fun isDying() = lampStatus == 2

    fun displayName(): String = lampName?.takeIf { it.isNotBlank() } ?: "长明灯"
}

/**
 * 供灯记录，对应服务端 lamp_record 表（lamp/getHistory 返回列表项）。
 * 每条都绑着一次 IAP 订单号，是恢复购买和客诉追溯的凭证。
 */
class LampRecordData {
    var id: Int = 0
    var accountId: Int = 0
    //灯具外观 id，0=默认灯
    var lampDressId: Int = 0
    //灯油 id（shop_dress_config.id，dressType=5）
    var oilDressId: Int = 0
    //供在哪尊佛像前（buddha_config.id），0=历史数据/佛前通用
    var buddhaId: Int = 0
    //佛像名称（服务端 join 得出，历史数据为空）
    var buddhaName: String? = null
    //灯油天数
    var oilDays: Int = 0
    //点燃时间
    var lightDate: String? = null
    //燃尽时间
    var expireDate: String? = null
    //1燃烧中 2已燃尽
    var lampStatus: Int = 0
    //本次点燃/续燃的 IAP 订单号
    var iapOrderNo: String? = null
    var createDate: String? = null
    //展示用：灯具外观名称（服务端 join 出，默认灯为空）
    var lampName: String? = null
    //展示用：灯油名称（如「七日灯油」）
    var oilName: String? = null
}

package com.fingertip.baselib.bean

/**
 * 念佛页当日进度（practice/getNianfoTodayExp）
 */
data class NianfoToday(
    //当日已获得修行值
    var exp: Int = 0,
    //当日已完成轮数（每轮=一藏=108声）
    var rounds: Int = 0
)

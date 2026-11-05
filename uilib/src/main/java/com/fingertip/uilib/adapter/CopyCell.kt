package com.fingertip.uilib.adapter

/**
 * 抄经格：char 是这一格要抄的原文，sourceIndex 是它在正文字符串里的下标（上报进度用）
 */
class CopyCell(val char: String, val sourceIndex: Int) {
    //用户选进去的字，空表示还没抄
    var typed: String = ""

    //是不是当前正在抄的这一格
    var current: Boolean = false

    //跳过的格子，字还在但不计功德
    var skipped: Boolean = false
}

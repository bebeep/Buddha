package com.fingertip.uilib.widgets.bottom_menu

import androidx.annotation.DrawableRes

class BottomMenuItem(
    @DrawableRes val menuRes: Int,
    val initSelect: Boolean = false,
    //数字未读角标（消息 tab 用）
    val hasMsg: Boolean = false,
    //红点角标（佛友圈用，原 count==3 硬编码改为声明式）
    val hasRedPoint: Boolean = false
)

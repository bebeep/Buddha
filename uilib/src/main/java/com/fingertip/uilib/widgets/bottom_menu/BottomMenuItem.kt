package com.fingertip.uilib.widgets.bottom_menu

import androidx.annotation.DrawableRes

class BottomMenuItem(
    @DrawableRes val menuRes: Int,
    val initSelect: Boolean = false,
    val hasMsg: Boolean = false
)
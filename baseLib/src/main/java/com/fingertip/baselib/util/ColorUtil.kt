package com.fingertip.baselib.util

import android.graphics.Color

object ColorUtil {

    /**
     * 动态设置透明度
     */
    fun changeAlpha(color:Int,fraction:Float):Int{
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val alpha = (Color.alpha(color) * fraction).toInt()
        return Color.argb(alpha,red, green, blue)
    }

}
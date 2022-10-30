package com.fingertip.baselib.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.blankj.utilcode.util.BarUtils

open class FullTopConstraintLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        initStatusBar()
    }

    fun initStatusBar() {
        val statusBarHeight = BarUtils.getStatusBarHeight()
        setPadding(paddingLeft, statusBarHeight, paddingRight, paddingBottom)
    }
}
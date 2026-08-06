package com.fingertip.baselib.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import com.blankj.utilcode.util.BarUtils

open class FullTopConstraintLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    /**
     * 在dispatchTouchEvent之前回调，用于处理键盘隐藏等逻辑
     * 返回true表示事件已被消费，不再向下分发
     */
    var onPreDispatchTouchListener: ((ev: MotionEvent) -> Boolean)? = null

    init {
        initStatusBar()
    }

    fun initStatusBar() {
        val statusBarHeight = BarUtils.getStatusBarHeight()
        setPadding(paddingLeft, statusBarHeight, paddingRight, paddingBottom)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            onPreDispatchTouchListener?.invoke(ev)?.let { consumed ->
                if (consumed) return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
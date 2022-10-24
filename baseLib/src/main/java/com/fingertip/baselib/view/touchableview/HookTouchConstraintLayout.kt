package com.fingertip.baselib.view.touchableview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout

class HookTouchConstraintLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    var callback: OnTouchCallback? = null

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        callback?.dispatchTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    interface OnTouchCallback {
        fun dispatchTouchEvent(ev: MotionEvent?)
    }
}
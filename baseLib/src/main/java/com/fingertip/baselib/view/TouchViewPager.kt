package com.fingertip.baselib.view

import androidx.viewpager.widget.ViewPager
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import java.lang.Exception

class TouchViewPager : ViewPager {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onInterceptTouchEvent(ev)
        } catch (e: Exception) {
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onTouchEvent(ev)
        } catch (e: Exception) {
        }
        return false
    }
}
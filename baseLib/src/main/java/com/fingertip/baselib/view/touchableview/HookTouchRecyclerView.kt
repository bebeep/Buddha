package com.fingertip.baselib.view.touchableview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lzlz.toplib.extention.toPx
import kotlin.math.abs

/**
 * 控制是否可触摸
 */
open class HookTouchRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var _startScroll = false
    private var _curDownX = 0f
    private var _curDownY = 0f
    private val _scrollMax = 8.toPx().toFloat()

    init {
        layoutManager = LinearLayoutManager(context)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val curX = e.x
        val curY = e.y
        if (e.action == MotionEvent.ACTION_DOWN) {
            _startScroll = false
            _curDownX = curX
            _curDownY = curY
        }
        if (e.action == MotionEvent.ACTION_MOVE) {
            _startScroll = Math.abs(curX - _curDownX) > _scrollMax || abs(curY - _curDownY) > _scrollMax
            super.onTouchEvent(e)
            return false
        }
        return if (e.action == MotionEvent.ACTION_UP && !_startScroll) {
            false
        } else super.onTouchEvent(e)
    }
}
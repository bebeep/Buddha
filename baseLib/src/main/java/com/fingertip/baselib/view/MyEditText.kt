package com.fingertip.baselib.view

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import com.fingertip.baseLib.R

open class MyEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MyTextView)
        val fontStyle = a.getInt(R.styleable.MyTextView_zdyFont, 0)
        val yStrike = a.getBoolean(R.styleable.MyTextView_ystrike, false)
        val yUnderline = a.getBoolean(R.styleable.MyTextView_ysunderline, false)
        a.recycle()

        when (fontStyle) {
            0 -> {
                super.setTypeface(Typeface.createFromAsset(getContext().assets, "gilroy-regular-3.otf"))
            }
            1 -> {
                super.setTypeface(Typeface.createFromAsset(getContext().assets, "gilroy-Medium-2.otf"))
            }
            2 -> {
                super.setTypeface(Typeface.createFromAsset(getContext().assets, "gilroy-bold-4.otf"))
            }
            3 -> {
                super.setTypeface(Typeface.createFromAsset(getContext().assets, "gilroy-blackitalic-7.otf"))
            }
        }

        if (yStrike) {
            paint.flags = Paint.STRIKE_THRU_TEXT_FLAG
        }

        if (yUnderline) {
            paint.flags = Paint.UNDERLINE_TEXT_FLAG
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.action) {
            MotionEvent.ACTION_MOVE -> {
                parent.requestDisallowInterceptTouchEvent(true)
            }
        }
        return super.onTouchEvent(event)
    }
}
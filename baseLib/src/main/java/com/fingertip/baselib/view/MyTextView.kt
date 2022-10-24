package com.fingertip.baselib.view

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.fingertip.baseLib.R


/**
 * 自定义字体文件
 */
open class MyTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MyTextView)
        val fontStyle = a.getInt(R.styleable.MyTextView_zdyFont, 0)
        val yStrike = a.getBoolean(R.styleable.MyTextView_ystrike, false)
        val yUnderline = a.getBoolean(R.styleable.MyTextView_ysunderline, false)
        a.recycle()

        setSize(fontStyle)

        if (yStrike) {
            paint.flags = Paint.STRIKE_THRU_TEXT_FLAG
        }

        if (yUnderline) {
            paint.flags = Paint.UNDERLINE_TEXT_FLAG
        }

       this.textDirection = View.TEXT_DIRECTION_LTR
    }

    fun setSize(fontStyle:Int){
        when (fontStyle) {
            0 -> {
                super.setTypeface(Typeface.createFromAsset(context.assets, "gilroy-regular-3.otf"))
            }
            1 -> {
                super.setTypeface(Typeface.createFromAsset(context.assets, "gilroy-Medium-2.otf"))
            }
            2 -> {
                super.setTypeface(Typeface.createFromAsset(context.assets, "gilroy-bold-4.otf"))
            }
            3 -> {
                super.setTypeface(Typeface.createFromAsset(context.assets, "gilroy-blackitalic-7.otf"))
            }
        }
    }
}
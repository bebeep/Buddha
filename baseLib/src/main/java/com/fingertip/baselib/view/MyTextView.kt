package com.fingertip.baselib.view

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.fingertip.baselib.R


/**
 * 自定义字体文件
 * 字体用静态缓存：Typeface.createFromAsset 每次调用都会重新打开并解析 ttf，
 * 抄经页一屏几十个格子全是 MyTextView，不缓存会导致进页明显卡顿
 */
open class MyTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        private val FONT_CACHE = HashMap<Int, Typeface>()

        fun cachedTypeface(context: Context, fontStyle: Int): Typeface? {
            val path = when (fontStyle) {
                0 -> "font/Montserrat-Medium.ttf"
                1 -> "font/Montserrat-SemiBold.ttf"
                2 -> "font/Montserrat-Bold.ttf"
                3 -> "font/Montserrat-ExtraBold.ttf"
                4 -> "font/Mont-BoldItalic.ttf"
                else -> return null
            }
            return FONT_CACHE[fontStyle] ?: run {
                try {
                    Typeface.createFromAsset(context.assets, path)
                } catch (e: Exception) {
                    null
                }?.let {
                    FONT_CACHE[fontStyle] = it
                    it
                }
            }
        }
    }

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

        includeFontPadding = false
    }

    fun setSize(fontStyle: Int) {
        cachedTypeface(context, fontStyle)?.let { super.setTypeface(it) }
    }
}

package com.fingertip.baselib.view.shapeview

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import com.fingertip.baseLib.R

class ShapeParser {

    var borderWidth = 0
    var borderColor = Color.BLACK
    var radius = 0f
    var bgColor = 0
    var startColor = 0
    var endColor = 0
    var gradientOrientation = 0
    var bgDrawable: Drawable? = null

    fun parseAttributes(context: Context, attrs: AttributeSet?, defStyleAttr: Int): Drawable {
        val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.GDView, defStyleAttr, 0)

        borderWidth = attributes.getDimensionPixelSize(R.styleable.GDView_gdBorderWidth, 0)
        borderColor = attributes.getColor(R.styleable.GDView_gdBorderColor, Color.BLACK)
        radius = attributes.getDimension(R.styleable.GDView_gdRadius, 0f)
        bgColor = attributes.getColor(R.styleable.GDView_gdBgColor, Color.TRANSPARENT)
        startColor = attributes.getColor(R.styleable.GDView_gdStartColor, Color.TRANSPARENT)
        endColor = attributes.getColor(R.styleable.GDView_gdEndColor, Color.TRANSPARENT)
        gradientOrientation = attributes.getInt(R.styleable.GDView_gdGradientOrientation, 0)
        bgDrawable = attributes.getDrawable(R.styleable.GDView_gdBgDrawable)

        if (attributes.hasValue(R.styleable.GDView_gdBgDrawable)){
            attributes.recycle()
            return bgDrawable as Drawable
        }else{
            attributes.recycle()
        }

        return buildDrawable()
    }

    fun buildDrawable(): Drawable {
        val gd: GradientDrawable//创建drawable
        if (startColor != Color.TRANSPARENT) {
            val orientation: GradientDrawable.Orientation =
                when(gradientOrientation) {
                    0 -> { GradientDrawable.Orientation.TOP_BOTTOM }
                    1 -> { GradientDrawable.Orientation.TR_BL }
                    2 -> { GradientDrawable.Orientation.RIGHT_LEFT }
                    3 -> { GradientDrawable.Orientation.BR_TL }
                    4 -> { GradientDrawable.Orientation.BOTTOM_TOP }
                    5 -> { GradientDrawable.Orientation.BL_TR }
                    6 -> { GradientDrawable.Orientation.LEFT_RIGHT }
                    else -> { GradientDrawable.Orientation.TL_BR }
                }
            gd = GradientDrawable(orientation, intArrayOf(startColor, endColor))
        } else {
            gd = GradientDrawable()
            gd.setColor(bgColor)
        }
        gd.cornerRadius = radius
        if (borderWidth > 0) {
            gd.setStroke(borderWidth, borderColor)
        }
        return gd
    }
}
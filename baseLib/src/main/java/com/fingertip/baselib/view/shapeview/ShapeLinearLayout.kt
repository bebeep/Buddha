package com.fingertip.baselib.view.shapeview

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

open class ShapeLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        init(context, attrs, defStyleAttr)
    }

    private lateinit var parser: ShapeParser

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        parser = ShapeParser()
        this.background = parser.parseAttributes(context, attrs, defStyleAttr)
    }

    fun setBgColor(color: Int) {
        parser.bgColor = color
        this.background = parser.buildDrawable()
    }
}

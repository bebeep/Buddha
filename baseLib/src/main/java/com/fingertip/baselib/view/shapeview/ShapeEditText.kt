package com.fingertip.baselib.view.shapeview

import android.content.Context
import android.util.AttributeSet
import com.fingertip.baseLib.R
import com.fingertip.baselib.view.MyEditText

/**
 * 圆角
 */
open class ShapeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : MyEditText(context, attrs, defStyleAttr) {

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

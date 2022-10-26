package com.fingertip.baselib.view.shapeview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.webkit.WebView
import com.lzlz.toplib.extention.toPx

/**
 * 通过绘制实现 圆角，适用所有view
 */
class ShapeWebView : WebView {
    private val TAG = "CusWebView"
    private val m_radius = 20.toPx().toFloat()
    private var mWidth = 100
    private var mHeight = 100
    private val radiusArray = floatArrayOf(m_radius, m_radius, m_radius, m_radius, 0f, 0f, 0f, 0f)

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {}
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //判断 避免 width，height值为0,否则Bitmap.createBitmap()报错
        if (measuredWidth != 0) {
            mWidth = measuredWidth
        }
        if (measuredHeight != 0) {
            mHeight = measuredHeight
        }
    }

    override fun onDraw(canvas: Canvas) {
        val x = this.scrollX
        val y = this.scrollY
        val path = Path()
        val rectF = RectF(x.toFloat(), y.toFloat(), (x + mWidth).toFloat(), (y + mHeight).toFloat())
        path.addRoundRect(rectF, radiusArray, Path.Direction.CW)
        canvas.clipPath(path)
        super.onDraw(canvas)
    }
}
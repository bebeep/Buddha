package com.fingertip.baselib.view.shapeview

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.fingertip.baseLib.R

open class ShapeImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var typedArray: TypedArray? = null
    private lateinit var rids: FloatArray

    init {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        typedArray = context.obtainStyledAttributes(attrs, R.styleable.CornerImageView)
        init()
    }


    fun init() {
        if (typedArray != null) {
            var rBottomLeft =
                typedArray!!.getDimensionPixelSize(R.styleable.CornerImageView_radius_b_l, 0)
            var rBottomRight =
                typedArray!!.getDimensionPixelSize(R.styleable.CornerImageView_radius_b_r, 0)
            var rTopLeft =
                typedArray!!.getDimensionPixelSize(R.styleable.CornerImageView_radius_t_l, 0)
            var rTopRight =
                typedArray!!.getDimensionPixelSize(R.styleable.CornerImageView_radius_t_r, 0)
            val radius = typedArray!!.getDimensionPixelOffset(R.styleable.CornerImageView_radius, 0)
            if (radius > 0) {
                rBottomLeft = radius
                rBottomRight = radius
                rTopLeft = radius
                rTopRight = radius
            }
            typedArray!!.recycle()
            /*圆角的半径，依次为左上角xy半径，右上角，右下角，左下角*/
            rids = floatArrayOf(
                rTopLeft.toFloat(),
                rTopLeft.toFloat(),
                rTopRight.toFloat(),
                rTopRight.toFloat(),
                rBottomRight.toFloat(),
                rBottomRight.toFloat(),
                rBottomLeft.toFloat(),
                rBottomLeft.toFloat()
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    private val path = Path()
    private val rectF = RectF()
    private val filter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override fun onDraw(canvas: Canvas) {
        val w = this.width - paddingStart - paddingEnd
        val h = this.height - paddingTop - paddingBottom
        /*向路径中添加圆角矩形。radii数组定义圆角矩形的四个圆角的x,y半径。radii长度必须为8*/rectF[(paddingStart + paddingEnd).toFloat(), (paddingTop + paddingBottom).toFloat(), w.toFloat()] =
            h.toFloat()
        path.addRoundRect(rectF, rids, Path.Direction.CW)
        canvas.clipPath(path)
        canvas.drawFilter = filter
        super.onDraw(canvas)
    }
}

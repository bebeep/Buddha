package com.fingertip.uilib.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CopyCell
import kotlin.math.ceil
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R as BaseR

/**
 * 抄经棋盘：一屏 60+ 格全部可见、无滚动，RecyclerView 的逐格 inflate
 * 会把进页帧顶到 100ms+（单帧全量创建 400+ View）。改用单个自绘 View 画整屏：
 * measure/layout 近零，数据变化只 invalidate；长按当前格跳过由手势定位。
 *
 * 视觉规格与原 item_fojing_copy.xml 等价：
 * 上半格 40dp 淡色原文（底对齐、paddingBottom 4dp），下半格 40dp 抄写字，
 * 格间距 5dp，田字格线 1dp。
 */
class CopyBoardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cells: List<CopyCell> = emptyList()
    private var column = 1

    //抄经显示设置，由页面设置后 refresh() 生效
    var defaultTextColor = 0x4D2C1900
    var copyTextColor = TopApplication.instance.getColor(BaseR.color.c_ink)
    var copyTextSize = 30f

    //原文（淡色）字号，随设置档位走
    var defaultTextSize = 22f

    //长按当前格（跳过）
    var onCellLongClick: (() -> Unit)? = null

    private val faintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sp(18f)
    }
    private val typedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val linePaint = Paint().apply { color = 0x26C4A265 }
    private val bgCurrent = AppCompatResources.getDrawable(context, R.drawable.bg_fojing_copy_answer)
    private val bgNormal = AppCompatResources.getDrawable(context, R.drawable.bg_fojing_copy_item)
    private val area = RectF()

    private val gesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            if (hitCurrent(e)) onCellLongClick?.invoke()
        }
    })

    /** 换一屏数据；同一屏内格子对象被原地修改后调 refresh() 即可 */
    fun submit(data: List<CopyCell>, col: Int) {
        cells = data
        column = col.coerceAtLeast(1)
        requestLayout()
        invalidate()
    }

    fun refresh() = invalidate()

    private fun rowCount() = if (column <= 0) 0 else ceil(cells.size / column.toDouble()).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val cellH = dp(40f + 40f + 5f)
        setMeasuredDimension(w, (rowCount() * cellH).toInt() + paddingTop + paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cells.isEmpty() || column <= 0) return

        faintPaint.color = defaultTextColor
        faintPaint.textSize = sp(defaultTextSize)
        typedPaint.textSize = sp(copyTextSize)
        val cellW = width.toFloat() / column
        val gap = dp(5f)
        val topH = dp(40f)
        val cellH = topH + dp(40f)
        val rows = rowCount()

        for ((i, cell) in cells.withIndex()) {
            val col = i % column
            val row = i / column
            val x0 = col * cellW
            val y0 = row * (cellH + gap)
            area.set(x0, y0 + topH, x0 + cellW, y0 + cellH)

            //田字格线：typed 区上下横线，左竖线；列尾或最后一格补右竖线
            canvas.drawLine(x0, area.top, area.right, area.top, linePaint)
            canvas.drawLine(x0, area.bottom, area.right, area.bottom, linePaint)
            canvas.drawLine(x0, area.top, x0, area.bottom, linePaint)
            if (col == column - 1 || i == cells.size - 1) {
                canvas.drawLine(area.right, area.top, area.right, area.bottom, linePaint)
            }

            //上半格淡色原文（底对齐，距 typed 区 4dp）
            val fm = faintPaint.fontMetrics
            canvas.drawText(cell.char, x0 + cellW / 2, area.top - dp(4f) - fm.descent, faintPaint)

            //下半格：当前格画红框米字格底
            (if (cell.current) bgCurrent else bgNormal)?.let {
                it.setBounds(area.left.toInt(), area.top.toInt(), area.right.toInt(), area.bottom.toInt())
                it.draw(canvas)
            }
            if (cell.typed.isNotEmpty()) {
                //跳过的格用浅色字，跟抄过的区分开
                typedPaint.color = if (cell.skipped) TopApplication.instance.getColor(BaseR.color.c_gray_ccc) else copyTextColor
                val tf = typedPaint.fontMetrics
                val baseline = area.top + (area.height() - tf.descent + tf.ascent) / 2 - tf.ascent
                canvas.drawText(cell.typed, x0 + cellW / 2, baseline, typedPaint)
            }
        }
    }

    private fun hitCurrent(e: MotionEvent): Boolean {
        if (column <= 0 || cells.isEmpty()) return false
        val cellW = width.toFloat() / column
        val step = dp(80f + 5f)
        val col = (e.x / cellW).toInt().coerceIn(0, column - 1)
        val row = (e.y / step).toInt().coerceIn(0, rowCount() - 1)
        val index = row * column + col
        return cells.getOrNull(index)?.current == true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gesture.onTouchEvent(event)
        return true
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)
}

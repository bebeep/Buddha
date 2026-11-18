package com.fingertip.uilib.view.nianfo

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * 「一藏·N」计数胶囊：文字 + 圆角矩形边框一体绘制，**尺寸严格按文字宽高包裹**。
 *
 * - 念佛中：完整边框包围文字（sweep = 1）；
 * - 停止念佛后调 [scheduleIdle]：先**静止 3s**（边框保持完整，期间继续念佛重置静止期），
 *   静止期结束再花 3s 沿周长把边框排空，结束回调里数字归 0 并隐藏整框；
 * - 期间再次念佛调 [reset] 取消静止/排空，边框恢复完整、继续累加。
 */
class NianfoCountFrame @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        //内边距：文字到边框的距离
        private const val PAD_H_DP = 18f
        private const val PAD_V_DP = 9f
    }

    var text: String = ""
        set(value) {
            if (field == value) return
            field = value
            requestLayout()
            invalidate()
        }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF6E9C8.toInt()
        textSize = resources.displayMetrics.density * 17
        letterSpacing = 0.18f
        textAlign = Paint.Align.CENTER
        setShadowLayer(10 * resources.displayMetrics.density, 0f, 0f, 0x4DE8CF94.toInt())
    }

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 1.4f
        color = 0x73E8CF94.toInt()   //边框：半透明暖金
    }

    private val rect = RectF()
    private val framePath = Path()
    private val drawPath = Path()
    private var measure: PathMeasure? = null

    //边框描画比例 0..1（1=完整包围，0=完全消失）
    private var sweep = 1f

    //静止期等待回调（停止念佛后先静止 holdMs，再开始排空）
    private var holdRunnable: Runnable? = null
    private var drainAnim: ValueAnimator? = null
    private var onIdleEnd: (() -> Unit)? = null

    /**
     * 停止念佛后的空闲流程：
     * ① 静止 [holdMs]（边框保持完整，期间继续念佛调 [reset] 会整体重置）→
     * ② 边框在 [drainMs] 内沿周长排空 → ③ 触发 [onEnd]（计数归零 + 文本隐藏）。
     */
    fun scheduleIdle(holdMs: Long, drainMs: Long, onEnd: () -> Unit) {
        cancelIdle()
        onIdleEnd = onEnd
        val r = Runnable { startDrain(drainMs) }
        holdRunnable = r
        postDelayed(r, holdMs)
    }

    /** 取消静止等待与排空动画，恢复完整边框（用户继续念佛） */
    fun reset() {
        cancelIdle()
        sweep = 1f
        invalidate()
    }

    private fun cancelIdle() {
        holdRunnable?.let { removeCallbacks(it) }
        holdRunnable = null
        drainAnim?.cancel()
        drainAnim = null
        onIdleEnd = null
    }

    /** 排空动画：sweep 1→0，结束回调 */
    private fun startDrain(durationMs: Long) {
        holdRunnable = null
        drainAnim?.cancel()
        drainAnim = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener {
                sweep = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    drainAnim = null
                    if (cancelled) return
                    val cb = onIdleEnd
                    onIdleEnd = null
                    cb?.invoke()
                }
            })
            start()
        }
    }

    private val padH = PAD_H_DP * resources.displayMetrics.density
    private val padV = PAD_V_DP * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val fm = textPaint.fontMetrics
        val textH = fm.descent - fm.ascent
        val w = (textPaint.measureText(text) + padH * 2).toInt()
        val h = (textH + padV * 2).toInt()
        setMeasuredDimension(
            resolveSize(w.coerceAtLeast(0), widthMeasureSpec),
            resolveSize(h.coerceAtLeast(0), heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) {
            measure = null
            return
        }
        val r = h / 2f
        //边框略内缩半根线宽，避免描边被裁
        val inset = framePaint.strokeWidth / 2f
        rect.set(inset, inset, w - inset, h - inset)
        framePath.reset()
        framePath.addRoundRect(rect, r, r, Path.Direction.CW)
        measure = PathMeasure(framePath, false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //文字：垂直居中
        val fm = textPaint.fontMetrics
        val baseline = height / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, width / 2f, baseline, textPaint)

        if (sweep <= 0f) return
        val m = measure ?: return
        val len = m.length
        drawPath.rewind()
        //沿周长描 sweep 比例的边框（sweep=1 时完整包围）
        m.getSegment(0f, len * sweep, drawPath, true)
        canvas.drawPath(drawPath, framePaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelIdle()
    }
}

package com.fingertip.uilib.view.earlypractice

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.max

/**
 * 早课礼佛：左右手按住区的水波荡漾动画。
 *
 * 三圈波纹错相位向外扩散，提示用户按住；用户按住后 [stopRipple] 冻结成静态光环，
 * 松手再 [startRipple]。动画只在 attached 且可见时跑，退页/隐藏即停，不留后台绘制。
 */
class EpRippleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x66E8CF94.toInt()
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x22E8CF94.toInt()
    }
    private var phase = 0f
    private var animator: ValueAnimator? = null

    var rippling = true
        private set

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxR = minOf(width, height) / 2f - 4f
        canvas.drawCircle(cx, cy, maxR * 0.42f, corePaint)
        for (i in 0 until 3) {
            val p = (phase + i / 3f) % 1f
            ringPaint.alpha = ((1f - p) * 110).toInt().coerceIn(0, 255)
            ringPaint.strokeWidth = max(1.5f, 3.5f * (1f - p))
            canvas.drawCircle(cx, cy, maxR * (0.28f + 0.72f * p), ringPaint)
        }
    }

    private fun startAnim() {
        if (animator != null || !rippling) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopAnim() {
        animator?.cancel()
        animator = null
    }

    fun startRipple() {
        rippling = true
        if (isAttachedToWindow && visibility == VISIBLE) startAnim()
        else invalidate()
    }

    fun stopRipple() {
        rippling = false
        stopAnim()
        phase = 0f
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (rippling) startAnim()
    }

    override fun onDetachedFromWindow() {
        stopAnim()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && rippling && isAttachedToWindow) startAnim() else stopAnim()
    }
}

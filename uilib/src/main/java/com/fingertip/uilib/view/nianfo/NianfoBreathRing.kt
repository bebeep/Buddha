package com.fingertip.uilib.view.nianfo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * 节拍呼吸环（对应原型 .breath）：按节奏时钟相位缩放，拍点窗口内点亮发光。
 * 每帧自驱动重绘（一个圆环，开销可忽略）。
 */
class NianfoBreathRing @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var rhythm: NianfoRhythm? = null

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
    }

    //光晕：低透明宽描边（原来每帧 setShadowLayer 重建大半径阴影，是掉帧主因之一）
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 16f * resources.displayMetrics.density
        color = Color.TRANSPARENT
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val clock = rhythm ?: return
        val info = clock.phaseInfo()
        val cx = width / 2f
        val cy = height / 2f
        val base = minOf(width, height) / 2f - ringPaint.strokeWidth
        //拍点收缩 → 放大
        val s = 0.9f + 0.16f * Math.cos(info.phase * 2.0 * Math.PI).toFloat()

        val beat = info.t < NianfoRhythm.PERFECT_WINDOW ||
                info.t > NianfoRhythm.HIT_ONE - NianfoRhythm.PERFECT_WINDOW
        if (beat) {
            glowPaint.color = 0x33E8CF94            //rgba(232,207,148,.2) 柔光
            ringPaint.color = 0xB3F6E9C8.toInt()    //rgba(246,233,200,.7)
        } else {
            glowPaint.color = Color.TRANSPARENT
            ringPaint.color = 0x38E8CF94.toInt()    //rgba(232,207,148,.22)
        }
        val r = base * s
        if (glowPaint.color != Color.TRANSPARENT) canvas.drawCircle(cx, cy, r, glowPaint)
        canvas.drawCircle(cx, cy, r, ringPaint)
        postInvalidateOnAnimation()
    }
}

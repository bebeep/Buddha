package com.fingertip.uilib.view.nianfo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 佛光粒子层（对应原型 canvas #fx）：金尘缓慢上浮 + 左右摇曳 + 明暗闪烁。
 *
 * 性能：光点用预渲染的径向渐变贴图（drawBitmap），不再每帧对每个粒子 setShadowLayer
 * （那是逐帧重建 Paint 特殊效果，粒子一多就掉帧）。
 */
class NianfoGlitterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private class Part(
        var x: Float, var y: Float, val r: Float, val speed: Float,
        val alpha: Float, val phase: Float, val sway: Float
    )

    private val parts = ArrayList<Part>()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private var spark: Bitmap? = null
    private var lastTs = 0L

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (spark == null) spark = buildSpark()
        rebuild()
    }

    /** 一张 32px 的软光点：中心实、边缘渐隐。 */
    private fun buildSpark(): Bitmap {
        val size = 32
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                size / 2f, size / 2f, size / 2f,
                intArrayOf(0xFFFFFFFF.toInt(), 0x88FFFFFF.toInt(), 0x00FFFFFF),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        c.drawCircle(size / 2f, size / 2f, size / 2f, p)
        return bmp
    }

    private fun rebuild() {
        parts.clear()
        val dm = resources.displayMetrics.density
        val n = min(44, (width * height / (14000f * dm * dm)).toInt())
        repeat(n) { parts.add(newPart(init = true)) }
    }

    private fun newPart(init: Boolean): Part {
        val dm = resources.displayMetrics.density
        return Part(
            x = Random.nextFloat() * width,
            y = if (init) Random.nextFloat() * height else height + 10 * dm,
            r = (0.6f + Random.nextFloat() * 1.8f) * dm,
            speed = (0.12f + Random.nextFloat() * 0.45f) * dm * 60f, //px/s
            alpha = 0.15f + Random.nextFloat() * 0.5f,
            phase = Random.nextFloat() * 6.28f,
            sway = (0.3f + Random.nextFloat() * 0.8f) * dm
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = spark ?: return
        val ts = System.currentTimeMillis()
        val dt = if (lastTs == 0L) 16L else min(50L, ts - lastTs)
        lastTs = ts
        for (i in parts.indices) {
            val p = parts[i]
            p.y -= p.speed * dt / 1000f
            p.x += sin(ts / 1400f + p.phase) * p.sway * 0.3f * dt / 16f
            if (p.y < -10 * resources.displayMetrics.density) {
                parts[i] = newPart(init = false)
                continue
            }
            val a = p.alpha * (0.55f + 0.45f * sin(ts / 900f + p.phase))
            paint.alpha = (a.coerceIn(0f, 1f) * 255).toInt()
            val r = p.r * 4f
            canvas.drawBitmap(bmp, null, android.graphics.RectF(p.x - r, p.y - r, p.x + r, p.y + r), paint)
        }
        postInvalidateOnAnimation()
    }
}

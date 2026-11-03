package com.fingertip.uilib.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 金色圆环进度，首页佛历头卡右侧的签到进度的位置。
 *
 * 只有一圈底环 + 一段金色进度弧 + 中间两行字，属性全部代码设置（页面只有一个地方用，
 * 没必要为它写一套自定义 attrs）：
 *   progress / max 决定弧长，text 大字，subText 小字。
 * 尺寸完全由布局给，onDraw 按短边取半径。
 */
class RingProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    //环宽，2dp 在暗底上足够清楚又不会显得笨重
    private val ringWidth = 2.5f * density

    private val rect = RectF()

    //底环：主金低透明
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
        color = Color.parseColor("#33C4A265")
    }

    //进度弧：亮金，圆头
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#C4A265")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 14f * density
        color = Color.parseColor("#E3C88B")
        isFakeBoldText = true
    }

    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 9f * density
        color = Color.parseColor("#8C8069")
    }

    //当前进度与满值，max<=0 时不画弧
    var progress = 0f
        set(value) {
            field = value
            invalidate()
        }

    var max = 7f
        set(value) {
            field = value
            invalidate()
        }

    //中间大字（如「签到」）
    var text = ""
        set(value) {
            field = value
            invalidate()
        }

    //大字下方小字（如「连签 12 天」），为空只画大字
    var subText = ""
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        //半径按短边算，留出半个环宽，避免描边被裁掉
        val radius = minOf(width, height) / 2f - ringWidth
        if (radius <= 0f) return

        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawCircle(cx, cy, radius, ringPaint)

        val ratio = if (max <= 0f) 0f else (progress / max).coerceIn(0f, 1f)
        if (ratio > 0f) {
            //从 12 点方向顺时针画
            canvas.drawArc(rect, -90f, 360f * ratio, false, progressPaint)
        }

        val hasSub = subText.isNotBlank()
        val mainY = cy + if (hasSub) -density else textPaint.textSize / 3f
        canvas.drawText(text, cx, mainY, textPaint)
        if (hasSub) {
            canvas.drawText(subText, cx, cy + 11f * density, subPaint)
        }
    }
}

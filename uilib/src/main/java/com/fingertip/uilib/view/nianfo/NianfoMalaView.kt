package com.fingertip.uilib.view.nianfo

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 佛珠环：18 个位置（1 佛头 + 17 子珠）。
 *
 * 转动规则：
 * - **所有珠子（含佛头）整体顺时针旋转**，佛头跟着环走；
 * - **高亮光圈固定在正上方**，珠子从它下面流过，作"念到哪了"的指示；
 * - 敲木鱼 / 点珠 → 顺时针推进一格（20°）；
 * - 拨动：**只在右侧半圆从上往下**才跟手，且**一次手势最多一格**（滑再多也只翻一颗）；
 * - 左侧或其它方向的滑动一律不响应。
 */
class NianfoMalaView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val BEADS = 18
        private const val ROT_STEP = 360f / BEADS
        //子珠暖蜜焦糖色（亮部/中部/暗部）
        private val CHILD_COLORS = intArrayOf(0xFFA9743F.toInt(), 0xFF6B4423.toInt(), 0xFF3B2413.toInt())
        //佛头更金
        private val GURU_COLORS = intArrayOf(0xFFE0BD6A.toInt(), 0xFF9A7433.toInt(), 0xFF5A3F18.toInt())
    }

    /** 点珠 / 敲击（含时机判定，走 doHit） */
    var onBeadTap: (() -> Unit)? = null

    /** 右侧拨过一格（纯计数，不判定、不敲木鱼形变） */
    var onBeadTurn: (() -> Unit)? = null

    var activeIdx = 0
        private set

    /** 累计顺时针格数（整数，作为 rotation 的基准） */
    private var stepCount = 0

    //当前显示角度（度，顺时针为正），拖拽时直接跟手
    private var rotation = 0f
    private var rotationAnim: ValueAnimator? = null

    private val beadPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x99000000.toInt() }
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2EFFFFFF }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
    }

    //轨道半径 / 珠半径
    private var orbitR = 0f
    private var beadR = 0f
    private var guruR = 0f

    private val centers = arrayOfNulls<FloatArray>(BEADS)
    //珠体径向渐变缓存：位置固定，只在尺寸变化时重建
    private val shaders = arrayOfNulls<RadialGradient>(BEADS)

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    //拨动状态
    private var dragging = false      //本次手势是否走拨动逻辑（只有右侧为 true）
    private var downX = 0f
    private var downY = 0f
    private var lastAng = 0f
    private var dragAccum = 0f        //本次手势累计顺时针角度（封顶一格）
    private var dragBaseRot = 0f      //手势起点的整格角度
    private var dragBaseSteps = 0     //手势起点的格数
    private var dragCounted = false   //本次手势是否已计过一声

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        val half = minOf(w, h) / 2f
        orbitR = half * 0.745f
        beadR = half * 0.083f
        guruR = half * 0.1f
        val cx = w / 2f
        val cy = h / 2f
        for (b in 0 until BEADS) {
            val ang = (b.toFloat() / BEADS) * Math.PI * 2 - Math.PI / 2
            val x = cx + (cos(ang) * orbitR).toFloat()
            val y = cy + (sin(ang) * orbitR).toFloat()
            centers[b] = floatArrayOf(x, y)

            val isGuru = b == 0
            val r = if (isGuru) guruR else beadR
            //径向渐变亮心偏向左上（-0.32,-0.40 半径 1.45r），原型同款质感
            shaders[b] = RadialGradient(
                x - r * 0.32f, y - r * 0.40f, r * 1.45f,
                if (isGuru) GURU_COLORS else CHILD_COLORS, null, Shader.TileMode.CLAMP
            )
        }
    }

    /** 敲击推进：整环顺时针转一格（动画 450ms） */
    fun advance() {
        stepCount++
        activeIdx = (activeIdx + 1) % BEADS
        animateTo(stepCount * ROT_STEP)
    }

    private fun animateTo(target: Float) {
        rotationAnim?.cancel()
        rotationAnim = ValueAnimator.ofFloat(rotation, target).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                rotation = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val now = System.currentTimeMillis()

        //高亮光圈：钉死在正上方（"念到哪了"的指示），不随环转动
        val top = centers[0] ?: return
        val pulse = 0.5f + 0.5f * sin(2.0 * Math.PI * now / 1700.0).toFloat()
        glowPaint.color = (((0.16f + 0.42f * pulse) * 255).toInt() shl 24) or 0xE8CF94.toInt()
        canvas.drawCircle(top[0], top[1], guruR * (1.55f + 0.25f * pulse), glowPaint)

        //所有珠子（含佛头）一起顺时针旋转，佛头跟着环走
        canvas.save()
        canvas.rotate(rotation, cx, cy)
        for (b in 0 until BEADS) {
            val c = centers[b] ?: continue
            val isGuru = b == 0
            val r = if (isGuru) guruR else beadR
            beadPaint.shader = shaders[b]
            canvas.drawCircle(c[0], c[1], r, beadPaint)
            if (isGuru) canvas.drawCircle(c[0], c[1], r, glossPaint)
            canvas.drawCircle(c[0], c[1], r * 0.17f, holePaint)
        }
        canvas.restore()

        //光圈呼吸需要持续重绘（shader 已缓存，每帧成本很低）
        postInvalidateOnAnimation()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cx = cx()
        val cy = cy()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val dx = event.x - cx
                val dy = event.y - cy
                val dist = hypot(dx, dy)
                //只有环上才接管；中心区放行给木鱼
                if (dist <= orbitR * 0.55f) return false

                downX = event.x
                downY = event.y
                dragAccum = 0f
                dragCounted = false
                lastAng = angOf(dx, dy)
                rotationAnim?.cancel()
                dragBaseSteps = (rotation / ROT_STEP).roundToInt()
                dragBaseRot = dragBaseSteps * ROT_STEP
                rotation = dragBaseRot
                stepCount = dragBaseSteps
                //右侧半圆才允许拨动；左侧按下照样消费事件，但不做任何响应
                dragging = dx > 0f
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                //左侧按下 / 已翻满一格：吃掉事件但不响应
                if (!dragging || dragAccum >= ROT_STEP) return true

                val ang = angOf(event.x - cx, event.y - cy)
                var d = ang - lastAng
                if (d > 180f) d -= 360f
                if (d < -180f) d += 360f
                //逆时针不响应；单帧大跳变丢弃
                if (d <= 0f || d >= 45f) return true

                lastAng = ang
                dragAccum = (dragAccum + d).coerceAtMost(ROT_STEP)
                rotation = dragBaseRot + dragAccum
                //滑满九成即计一声（封顶后不再计，保证一次手势只翻一颗）
                if (!dragCounted && dragAccum >= ROT_STEP * 0.9f) {
                    dragCounted = true
                    activeIdx = (activeIdx + 1) % BEADS
                    onBeadTurn?.invoke()
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val moved = hypot(event.x - downX, event.y - downY) > touchSlop
                val cancelled = event.actionMasked == MotionEvent.ACTION_CANCEL
                dragging = false

                if (!moved && !cancelled) {
                    //点一下（左右都算）：走敲击判定，advance() 由 doHit 内部负责转珠
                    performClick()
                    onBeadTap?.invoke()
                    return true
                }
                if (cancelled) {
                    //取消：回到起点，不计数
                    stepCount = dragBaseSteps
                    animateTo(stepCount * ROT_STEP)
                    return true
                }
                //滑过一次：过半格算一格，否则退回原位
                val stepped = dragAccum >= ROT_STEP * 0.4f
                if (stepped && !dragCounted) {
                    dragCounted = true
                    activeIdx = (activeIdx + 1) % BEADS
                    onBeadTurn?.invoke()
                }
                stepCount = dragBaseSteps + if (stepped) 1 else 0
                animateTo(stepCount * ROT_STEP)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /** 屏幕坐标角度（度）：右侧从上往下 = -90 → +90，即顺时针为正 */
    private fun angOf(dx: Float, dy: Float): Float =
        Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

    private fun cx() = width / 2f
    private fun cy() = height / 2f
}

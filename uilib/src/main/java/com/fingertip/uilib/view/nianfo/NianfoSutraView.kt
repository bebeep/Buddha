package com.fingertip.uilib.view.nianfo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import kotlin.math.cos

/**
 * 经文层：铺在禅盘（佛珠区域）底部，作为"若隐若现"的经文背景。
 *
 * 性能：**先把整段竖排经文渲染进一张离屏位图（只做一次）**，运行时每帧仅
 * 1 次 saveLayer + 1 次 drawBitmap（纵向平移即滚动）+ 1 次遮罩贴图（边缘羽化）。
 * 相比逐字 drawText（200+ 次/帧，正是之前卡顿的原因），开销可以忽略。
 *
 * 纹理内部纵向两段内容完全相同，滚动位移取 [-h, 0] 循环，接缝不可见。
 * 边缘羽化用**方形四边渐变遮罩**（不是径向圆——圆罩会留下一个椭圆光斑，很难看）。
 */
class NianfoSutraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private val SUTRAS = arrayOf(
            "观自在菩萨行深般若波罗蜜多时照见五蕴皆空度一切苦厄",
            "色不异空空不异色色即是空空即是色受想行识亦复如是",
            "南无阿弥陀佛南无观世音菩萨南无大势至菩萨",
            "诸法空相不生不灭不垢不净不增不减",
            "心无挂碍无挂碍故无有恐怖远离颠倒梦想究竟涅槃",
            "揭谛揭谛波罗揭谛波罗僧揭谛菩提萨婆诃"
        )
        private const val COLUMN_COUNT = 6
        private const val COLOR_TEXT = 0xFFF0DCAE.toInt()

        //透明度：整体只做"若隐若现"——大约是上一版的四成，最亮也不到 0.3
        private const val ALPHA_MIN = 0.055f
        private const val ALPHA_MAX = 0.235f

        private const val DRIFT_SEC = 19f    //走完一屏的秒数
        private const val BREATHE_SEC = 5.4f //呼吸周期

        //离屏纹理降采样比例（越小越省内存，绘制时放大回来，天然再柔化一层）
        private const val TEX_SCALE = 0.5f
        private const val TWO_PI = (Math.PI * 2).toFloat()
    }

    private val dm = resources.displayMetrics.density

    private var tex: Bitmap? = null

    //贴图（带过滤，放大时平滑）
    private val texPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()

    private var running = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) buildTexture(w, h)
    }

    /**
     * 把竖排经文烧进纹理：纵向两段内容一致（滚动无缝），字符带模糊（柔化边缘）。
     */
    private fun buildTexture(w: Int, h: Int) {
        tex?.recycle()
        val tw = (w * TEX_SCALE).toInt().coerceAtLeast(2)
        val th = (h * 2 * TEX_SCALE).toInt().coerceAtLeast(4) //两段拼接
        val segH = th / 2f                                     //单段高度

        val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT
            //模糊让字迹虚化，一圈软边，不会像硬贴上去的
            maskFilter = BlurMaskFilter(1.5f * dm * TEX_SCALE, BlurMaskFilter.Blur.NORMAL)
        }

        for (i in 0 until COLUMN_COUNT) {
            val x = tw * (0.11f + i * 0.156f)
            val size = (13f + (i % 3) * 1.6f) * dm * TEX_SCALE
            val spacing = size * 1.34f
            val n = (segH / spacing).toInt().coerceAtLeast(1)
            val realSpacing = segH / n  //微调字距让每段正好排满 n 个字，接缝不出空档

            paint.textSize = size
            //列间亮度错落，避免一片死平
            val colAlpha = 0.55f + (i % 3) * 0.18f
            paint.alpha = (colAlpha * 255).toInt().coerceIn(0, 255)

            val src = SUTRAS[i % SUTRAS.size]
            for (seg in 0 until 2) {
                val baseY = seg * segH
                for (k in 0 until n) {
                    val y = baseY + (k + 0.62f) * realSpacing
                    c.drawText(src[k % src.length].toString(), x, y, paint)
                }
            }
        }

        tex = bmp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = tex ?: return
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        val t = SystemClock.uptimeMillis() / 1000f

        //滚动：0 → -h 循环（纹理上下两段相同，跳回时完全吻合）
        val scroll = -((t % DRIFT_SEC) / DRIFT_SEC) * h
        //呼吸：明暗轻微起伏
        val bx = (t % BREATHE_SEC) / BREATHE_SEC
        val breath = 0.5f - 0.5f * cos(bx * TWO_PI)
        texPaint.alpha = ((ALPHA_MIN + (ALPHA_MAX - ALPHA_MIN) * breath) * 255f)
            .toInt().coerceIn(0, 255)

        //dst 始终覆盖整个区域（纹理高 2h，位移最多 -h）
        dst.set(0f, scroll, w.toFloat(), scroll + h * 2f)
        canvas.drawBitmap(bmp, null, dst, texPaint)
    }

    private fun startAnim() {
        if (running) return
        running = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopAnim() {
        if (!running) return
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE && windowVisibility == VISIBLE) startAnim()
    }

    override fun onDetachedFromWindow() {
        stopAnim()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && isAttachedToWindow && windowVisibility == VISIBLE) {
            startAnim()
        } else {
            stopAnim()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && isAttachedToWindow && this.visibility == VISIBLE) {
            startAnim()
        } else {
            stopAnim()
        }
    }
}

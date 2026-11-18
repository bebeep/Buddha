package com.fingertip.uilib.view.earlypractice

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * 早课礼佛的「俯身拜」检测。
 *
 * 双手按住瞬间记录重力方向基准，之后手机**任意方向**倾斜（前倾 / 后仰 / 侧倾）
 * 相对基准偏满 [TILT_DEG] 度即触发一拜；回到基准 [REARM_DEG] 度内重新武装，可做下一拜。
 *
 * 用重力向量的三维夹角而非某个单轴分量，避免轴向弄反（旧实现误用 atan2(-x, …)
 * 测左右倾斜，而前后俯仰时 x≈0，导致完全不触发）；也不依赖陀螺仪硬门槛。
 *
 * 只在双手按住时注册，松手/切页/退页立即注销。
 */
class EpTiltDetector(private val context: Context, private val onBow: () -> Unit) :
    SensorEventListener {

    companion object {
        //触发一拜所需的倾斜角（度），留 2° 容错
        private const val TILT_DEG = 28f
        //回到基准多少度内重新武装
        private const val REARM_DEG = 10f
        //两次触发之间的冷却，避免一次动作连续计多拜
        private const val COOLDOWN_MS = 1000L
    }

    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    //按住瞬间的单位重力向量基准
    private var baseline: FloatArray? = null
    private var armed = true
    private var lastBowAt = 0L

    val supported: Boolean get() = accelerometer != null

    fun start() {
        if (manager == null || accelerometer == null) return
        baseline = null
        armed = true
        lastBowAt = 0L
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        manager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val norm = sqrt(x * x + y * y + z * z)
        if (norm < 1f) return
        val nx = x / norm
        val ny = y / norm
        val nz = z / norm

        val base = baseline
        if (base == null) {
            baseline = floatArrayOf(nx, ny, nz)
            return
        }

        // 当前姿态与基准的三维夹角（度）——任意方向倾斜都算
        val dot = (nx * base[0] + ny * base[1] + nz * base[2]).coerceIn(-1f, 1f)
        val angle = Math.toDegrees(acos(dot.toDouble())).toFloat()

        if (angle < REARM_DEG) {
            // 回到基准附近：重新武装，并缓慢跟随基准以吸收握持姿势的自然漂移
            armed = true
            val bx = base[0] * 0.9f + nx * 0.1f
            val by = base[1] * 0.9f + ny * 0.1f
            val bz = base[2] * 0.9f + nz * 0.1f
            val bn = sqrt(bx * bx + by * by + bz * bz)
            baseline = floatArrayOf(bx / bn, by / bn, bz / bn)
            return
        }

        val now = System.currentTimeMillis()
        if (armed && angle >= TILT_DEG && now - lastBowAt >= COOLDOWN_MS) {
            armed = false
            lastBowAt = now
            onBow()
        }
    }
}

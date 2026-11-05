package com.fingertip.uilib.view

/**
 * 佛前场景画布坐标换算（1080x2400 素材画布 ↔ 全屏视图坐标）
 *
 * 场景所有素材（佛像 cutout、佛龛、佛灯、香烛、贡品、Lottie 动画）都是
 * 1080x2400 全画布透明 PNG/JSON，位置在服务端素材合成时定死；客户端统一用
 * centerCrop 铺满屏幕叠层，所以视图坐标 = 居中偏移 + 画布坐标 * scale。
 *
 * 两个必须换算的地方（用画布中心当 pivot 会出错）：
 * 1. 局部缩放：灯焰随灯油变小时要以灯口为 pivot，否则火焰会整块上下漂移脱离灯口；
 *    香烛随供佛剩余时长变短要以香底为 pivot 向上缩。
 * 2. 点击命中：每层都是满屏控件，点击点在哪个控件上都一样，只能按画布坐标
 *    反算出手指点到底落在场景的哪个部件上（见 [hitScene]）。
 */
object SceneGeometry {

    /** 场景素材画布宽（与 compose_scene.py 输出一致） */
    const val CANVAS_W = 1080f

    /** 场景素材画布高 */
    const val CANVAS_H = 2400f

    /** 灯焰与香烟所在画布 x：灯在 x=150，右侧灯镜像到 1080-150=930 */
    const val LAMP_CX = 150f

    /** 灯口画布 y（灯体 h=230、底 2085，gen_flame_v5.py: base = lamp_top + 6 = 1861） */
    const val LAMP_MOUTH_Y = 1861f

    /** 香杆底画布 y（炉口内）：香随供佛剩余时长变短的缩放支点；香炉层固定不缩放 */
    const val INCENSE_BASE_Y = 2090f

    const val HIT_NONE = 0
    const val HIT_LAMP = 1
    const val HIT_SHRINE = 2

    /** centerCrop 缩放比：取宽高两向缩放的最大值 */
    private fun scale(viewW: Int, viewH: Int): Float {
        if (viewW <= 0 || viewH <= 0) return 1f
        return maxOf(viewW / CANVAS_W, viewH / CANVAS_H)
    }

    /** 画布 y → 视图 y */
    fun toViewY(viewW: Int, viewH: Int, canvasY: Float): Float {
        val s = scale(viewW, viewH)
        return (viewH - CANVAS_H * s) / 2f + canvasY * s
    }

    /** 视图坐标 → 画布坐标（点击命中用） */
    fun toCanvasY(viewW: Int, viewH: Int, viewY: Float): Float {
        val s = scale(viewW, viewH)
        return (viewY - (viewH - CANVAS_H * s) / 2f) / s
    }

    fun toCanvasX(viewW: Int, viewH: Int, viewX: Float): Float {
        val s = scale(viewW, viewH)
        return (viewX - (viewW - CANVAS_W * s) / 2f) / s
    }

    /**
     * 点击命中判定（画布坐标系，数值取自 out/anchors.json）：
     * - 佛灯：左右两侧灯体矩形，x 0..300 / 780..1080，y 1800..2220
     * - 佛龛：底部整条横带 y>=1780（龛座与香烛贡品所在区域），
     *   外加左右两侧龛框立柱（高档龛比佛像高，框边露在佛像两侧）
     * 命中优先级：佛灯 > 佛龛（灯叠在龛上面，点灯不该跳龛）
     */
    fun hitScene(viewW: Int, viewH: Int, x: Float, y: Float): Int {
        if (viewW <= 0 || viewH <= 0) return HIT_NONE
        val cx = toCanvasX(viewW, viewH, x)
        val cy = toCanvasY(viewW, viewH, y)
        if (cy < 0 || cy > CANVAS_H) return HIT_NONE

        // 佛灯：供桌后方（y 1855..2085 + 上方灯焰余量），左侧灯体(cx=120,宽~200)与右侧镜像
        if (cy in 1700f..2140f) {
            if (cx in 0f..260f || cx in (CANVAS_W - 260f)..CANVAS_W) return HIT_LAMP
        }
        // 佛龛底带
        if (cy >= 1780f) return HIT_SHRINE
        // 佛龛左右立柱（龛比佛像宽，两侧各露一条边框）
        if (cy >= 1300f && (cx <= 80f || cx >= CANVAS_W - 80f)) return HIT_SHRINE
        return HIT_NONE
    }
}

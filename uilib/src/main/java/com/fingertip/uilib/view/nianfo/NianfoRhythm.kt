package com.fingertip.uilib.view.nianfo

/**
 * 念佛页节奏时钟：一轮呼吸 CYCLE ms，每半轮一个可叩击拍点。
 * 敲击时用「距最近拍点的毫秒数」判定：净念(+2) / 专注(+1) / 随念(+1)。
 * 同一份时间基准喂给呼吸环与判定，保证视觉节拍与判定一致。
 */
class NianfoRhythm {

    companion object {
        //一轮呼吸时长 ms（缓慢，利静心）
        const val CYCLE = 2400L
        //两次可叩击点的间隔（吸/呼各一次）
        const val HIT_ONE = CYCLE / 2

        //命中窗口：拍点前后 160ms 内算「净念」
        const val PERFECT_WINDOW = 160L
        //430ms 内算「专注」，其余「随念」
        const val GOOD_WINDOW = 430L
    }

    //起始时间戳（ms）。进页即起拍，无需手动 start
    private val startAt = System.currentTimeMillis()

    /** 距起始的毫秒数 */
    fun elapsed(now: Long = System.currentTimeMillis()): Long = now - startAt

    /** 当前半轮内的相位 t（0..HIT_ONE）与距最近拍点的毫秒数 d */
    fun phaseInfo(now: Long = System.currentTimeMillis()): PhaseInfo {
        val t = elapsed(now) % HIT_ONE
        return PhaseInfo(t / HIT_ONE.toFloat(), minOf(t, HIT_ONE - t), t)
    }

    /** 敲击判定：返回 (等级文案, 加的修行值) */
    fun judge(): Pair<String, Int> {
        val d = phaseInfo().d
        return when {
            d < PERFECT_WINDOW -> "净念 · +2" to 2
            d < GOOD_WINDOW -> "专注 · +1" to 1
            else -> "随念 · +1" to 1
        }
    }

    data class PhaseInfo(val phase: Float, val d: Long, val t: Long)
}

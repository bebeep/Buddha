package com.fingertip.uilib.utils

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.fingertip.baselib.util.LevelUtil
import com.fingertip.baselib.util.loadImg

/**
 * 等级视觉统一出口（界面侧）
 *
 * 等级在界面上有两种形态：
 * 1. 内联徽章 [bindTag]：跟在昵称/头衔旁边的小药丸图，lv 数字画在图里，
 *    服务端按级下发（user_level_config.tagUrl，每级一张），全 App 各处统一用它
 * 2. 文字 [tagTitle]：徽章旁边的头衔（如「三阶·居士」），配置缺失时退化为「lvN」
 *
 * 颜色兜底全部来自 LevelUtil（境界段配色）：tagUrl 没配图时用段色画一个素药丸底，
 * 保证任何等级都有稳定的视觉标识。
 */
object LevelUi {

    /** 旧接口保留：昵称后面的等级串，如「lv3 三阶·善信」 */
    fun levelText(level: Int): String = LevelUtil.nameOf(level)

    /** 徽章旁的头衔文字，如「三阶·居士」；配置缺失时退化为「lv3」 */
    fun tagTitle(level: Int): String = LevelUtil.tagOf(level).ifBlank { "lv$level" }

    /**
     * 等级徽章图：服务端按级下发的药丸图（lv 数字已画在图里）
     *
     * 没拿到配置时用段色画一个本地药丸底兜底（头衔由 [tagTitle] 显示 lvN），
     * 保证 globalParam 未就绪时界面不至于空一块。
     */
    fun bindTag(iv: ImageView, level: Int) {
        val url = LevelUtil.tagUrlOf(level)
        if (url.isBlank()) {
            val d = iv.resources.displayMetrics.density
            iv.setImageDrawable(null)
            iv.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 21f * d
                setColor(LevelUtil.badgeFillOf(level))
                setStroke((1.5f * d).toInt().coerceAtLeast(1), LevelUtil.colorOf(level))
            }
            return
        }
        iv.background = null
        iv.loadImg(url)
    }

    /**
     * 给普通 TextView 套一层小徽章底（三级页配置缺失时的兜底样式）
     * @param horizontalPadDp 左右内边距，列表里字小就给 4，详情页给 8
     */
    fun applyTextBadge(
        tv: TextView,
        level: Int,
        radiusDp: Float = 4f,
        horizontalPadDp: Int = 5,
        withText: Boolean = true
    ) {
        val d = tv.resources.displayMetrics.density
        tv.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * d
            setColor(LevelUtil.badgeFillOf(level))
            setStroke((1 * d).toInt().coerceAtLeast(1), LevelUtil.colorOf(level))
        }
        tv.setTextColor(LevelUtil.colorOf(level))
        tv.setPadding(
            (horizontalPadDp * d).toInt(),
            (1 * d).toInt(),
            (horizontalPadDp * d).toInt(),
            (1 * d).toInt()
        )
        if (withText) tv.text = LevelUtil.badgeTextOf(level)
    }

    /**
     * 圆形等级标志：背景框画在 [bg] 上，数字写进 [tv]
     * [bg] 一般是个 View（宽高相等），[tv] 压在它中间
     */
    fun applyIconBadge(bg: View, tv: TextView?, level: Int, strokeDp: Float = 2f) {
        val d = bg.resources.displayMetrics.density
        bg.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(LevelUtil.badgeFillOf(level))
            setStroke(
                (strokeDp * d).toInt().coerceAtLeast(1),
                LevelUtil.colorOf(level)
            )
        }
        tv ?: return
        tv.setTextColor(LevelUtil.colorOf(level))
        tv.text = LevelUtil.badgeTextOf(level)
    }
}

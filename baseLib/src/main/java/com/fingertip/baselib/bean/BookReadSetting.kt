package com.fingertip.baselib.bean

/**
 * 读经显示设置，对应服务端 book_read_setting，一个账号一份。
 * 四档颜色/字号/行距只存编号，具体色值、sp、dp 在客户端映射
 */
class BookReadSetting {

    companion object {
        //四档正文色，与 bg_text_color_1~4 一致
        private val TEXT_COLORS = intArrayOf(
            0xFF2C1900.toInt(),
            0xFFFF9600.toInt(),
            0xFF51B23D.toInt(),
            0xFFAAAAAA.toInt()
        )

        //四档背景色，与 bg_color_1~4 一致
        private val BACKGROUND_COLORS = intArrayOf(
            0xFFFFFEFA.toInt(),
            0xFF2C1900.toInt(),
            0xFF4DD3E1C6.toInt(),
            0xFF4DEAE3D9.toInt()
        )

        //四档正文字号，第1档是和原来一样的 16sp
        private val FONT_SIZES = floatArrayOf(16f, 19f, 23f, 27f)

        //四档行间距附加值（dp），第2档是和原来一样的 10dp
        private val LINE_SPACINGS = floatArrayOf(4f, 10f, 16f, 22f)

        fun textColor(level: Int): Int = TEXT_COLORS[level.coerceIn(1, 4) - 1]

        fun backgroundColor(level: Int): Int = BACKGROUND_COLORS[level.coerceIn(1, 4) - 1]

        fun fontSize(level: Int): Float = FONT_SIZES[level.coerceIn(1, 4) - 1]

        fun lineSpacing(level: Int): Float = LINE_SPACINGS[level.coerceIn(1, 4) - 1]
    }

    var accountId: Int = 0

    //正文颜色编号 1-4
    var textColor: Int = 1

    //背景编号 1-4
    var backgroundColor: Int = 1

    //字号档位 1-4
    var fontSizeLevel: Int = 1

    //行距档位 1-4
    var lineSpacingLevel: Int = 2

    //阅读时常亮 0否 1是
    var keepScreenOn: Int = 1

    //正文显示繁体 0否 1是
    var showTrad: Int = 0

    fun contentTextColor(): Int = textColor(textColor)

    fun bgColor(): Int = backgroundColor(backgroundColor)

    fun contentTextSize(): Float = fontSize(fontSizeLevel)

    fun contentLineSpacing(): Float = lineSpacing(lineSpacingLevel)

    /**
     * 深色底（第2档深褐）就是夜间模式，标题要反白
     */
    fun isNight(): Boolean = backgroundColor == 2

    fun titleColor(): Int = if (isNight()) 0xFFFFFEFA.toInt() else 0xFF2C1900.toInt()

    fun keepOn(): Boolean = keepScreenOn == 1

    fun useTrad(): Boolean = showTrad == 1

    /**
     * 深色底（第2档深褐）上正文要用亮色，不然选了深字直接看不见
     */
    fun contentTextColorOnBg(): Int {
        if (backgroundColor != 2) return contentTextColor()
        //深褐底下正文文字色档位若也是深色，统一改用米白
        val color = contentTextColor()
        return if (!isLight(color)) 0xFFFFFEFA.toInt() else color
    }

    private fun isLight(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000 > 150
    }
}

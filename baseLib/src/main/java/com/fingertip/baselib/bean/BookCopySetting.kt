package com.fingertip.baselib.bean

/**
 * 抄经显示设置，对应服务端 book_copy_setting，一个账号一份。
 * 四档颜色和字号只存编号，具体色值/sp 在客户端映射
 */
class BookCopySetting {

    companion object {
        //四档文字色，与 bg_text_color_1~4 一致
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

        //四档字号，第3档是原来的 30sp
        private val FONT_SIZES = floatArrayOf(22f, 26f, 30f, 34f)

        fun textColor(level: Int): Int = TEXT_COLORS[level.coerceIn(1, 4) - 1]

        fun backgroundColor(level: Int): Int = BACKGROUND_COLORS[level.coerceIn(1, 4) - 1]

        fun fontSize(level: Int): Float = FONT_SIZES[level.coerceIn(1, 4) - 1]
    }

    var accountId: Int = 0

    //原文颜色编号 1-4
    var defaultColor: Int = 1

    //抄文颜色编号 1-4
    var copyColor: Int = 1

    //背景编号 1-4
    var backgroundColor: Int = 1

    //字号档位 1-4
    var fontSizeLevel: Int = 3

    //原文是淡色，固定 30% 不透明度，设置里只调色相
    fun defaultTextColor(): Int = (textColor(defaultColor) and 0x00FFFFFF) or (0x4D shl 24)

    fun copyTextColor(): Int = textColor(copyColor)

    fun bgColor(): Int = backgroundColor(backgroundColor)

    fun copyTextSize(): Float = fontSize(fontSizeLevel)
}

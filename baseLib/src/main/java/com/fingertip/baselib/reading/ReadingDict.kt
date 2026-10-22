package com.fingertip.baselib.reading

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan


/**
 * 读音条目
 * mark 为 true 表示这个字词按现在的读法和逐字念不一样（多音、异形、生僻），
 * 正文里给它加下划线，提醒用户可以点。
 * audio 是 assets/reading/audio 下的文件名，空字符串表示这个词没有录音
 */
class ReadingEntry(
    val word: String,
    val reading: String,
    val kind: String,
    val mark: Boolean,
    val audio: String = ""
)

/**
 * 读音库。数据打包在 assets/reading/reading_dict.tsv，简繁各一份别名，
 * 一条一行：字词 / 拼音 / 类别 / 是否标记 / 音频文件名
 */
object ReadingDict {

    private const val ASSET = "reading/reading_dict.tsv"

    //词条最长按 6 个字匹配，再长的是咒语整句，点了也没多大意义
    private const val MAX_WORD = 6

    private val map = HashMap<String, ReadingEntry>()

    @Volatile
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            try {
                context.assets.open(ASSET).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        if (line.isEmpty()) return@forEach
                        val parts = line.split('\t')
                        if (parts.size < 4) return@forEach
                        val entry = ReadingEntry(
                            parts[0],
                            parts[1],
                            parts[2],
                            parts[3] == "1",
                            if (parts.size > 4) parts[4] else ""
                        )
                        map[entry.word] = entry
                    }
                }
                loaded = true
            } catch (e: Exception) {
                //读音库读不出来就当没有，不影响看经
            }
        }
    }

    fun isReady(): Boolean = loaded

    /**
     * 查点击位置的字词读音。
     * 先按最长词匹配（点击的字可能落在词中间），都没有再退回单字
     */
    fun lookup(text: CharSequence, index: Int): ReadingEntry? {
        if (map.isEmpty() || index < 0 || index >= text.length) return null
        for (len in MAX_WORD downTo 2) {
            //点击字在词里的位置从第一个到最后一个都试一遍
            for (offset in 0 until len) {
                val start = index - offset
                if (start < 0 || start + len > text.length) continue
                val entry = map[text.subSequence(start, start + len).toString()]
                if (entry != null) return entry
            }
        }
        return map[text[index].toString()]
    }

    /**
     * 给读音特殊的词加下划线。单字不加，不然正文满屏都是线
     */
    fun mark(text: CharSequence): CharSequence {
        if (map.isEmpty()) return text
        val spannable = SpannableString(text)
        var i = 0
        while (i < text.length) {
            val len = markedLengthAt(text, i)
            if (len > 0) {
                spannable.setSpan(
                    UnderlineSpan(), i, i + len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                i += len
            } else {
                i++
            }
        }
        return spannable
    }

    private fun markedLengthAt(text: CharSequence, start: Int): Int {
        for (len in MAX_WORD downTo 2) {
            if (start + len > text.length) continue
            val entry = map[text.subSequence(start, start + len).toString()] ?: continue
            if (entry.mark) return len
        }
        return 0
    }
}

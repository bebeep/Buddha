package com.fingertip.baselib.bean

import com.fingertip.baselib.util.clearFirstLine
import java.lang.StringBuilder

/**
 * 敏感词
 */
class SensitiveWordsBean:TopData() {
    var emoji = ""
    var en = ""
    var ar = ""
    var msg_en=""
    var msg_ar=""


    fun checkSensitiveWords(text:String):Boolean{
        return text.clearFirstLine().contains("****")
    }


    fun getSensitiveWords():String{
        val sb = StringBuilder()
        sb.append("(?i)")
        if (en.isNotEmpty()){
            sb.append(en.replace(" ","|"))
        }
        if (ar.isNotEmpty()) {
            if (!sb.toString().endsWith("|"))sb.append("|")
            sb.append(ar.replace(" ","|"))
        }
        if (emoji.isNotEmpty()) {
            if (!sb.toString().endsWith("|"))sb.append("|")
            sb.append(emoji.replace(" ","|"))
        }
        return sb.toString()
    }
}
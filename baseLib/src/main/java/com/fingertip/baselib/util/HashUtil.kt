package com.fingertip.baselib.util

import com.fingertip.baselib.log
import okhttp3.internal.and
import java.io.UnsupportedEncodingException
import java.lang.StringBuilder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object HashUtil {

    fun getMD5(string: String): String {
        var hash: ByteArray? = null
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.toByteArray(charset("UTF-8")))
        } catch (e: NoSuchAlgorithmException) {
            log("HashUtil", e.toString())
        } catch (e: UnsupportedEncodingException) {
            log("HashUtil", e.toString())
        }
        val hex = StringBuilder(hash!!.size * 2)
        for (b in hash) {
            if (b and 0xFF < 0x10) hex.append("0")
            hex.append(Integer.toHexString(b and 0xFF))
        }
        return hex.toString()
    }
}
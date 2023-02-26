package com.fingertip.baselib

import android.util.Log
import com.fingertip.baseLib.BuildConfig


var ISDEBUG:Boolean
    get() {
        return BuildConfig.DEBUG
    }
    set(value) {}

fun log(tag: String? ="TAG", value: Any) {
    if (ISDEBUG) {
        Log.d( tag, value.toString())
    }
}

fun loge(tag: String?="TAG", value: Any) {
    if (ISDEBUG) {
        Log.e( tag, value.toString())
    }
}

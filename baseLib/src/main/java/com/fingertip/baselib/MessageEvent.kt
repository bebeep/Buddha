package com.fingertip.baselib

class MessageEvent {
    var what = 0
    var `object`: Any? = null

    constructor(what: Int, `object`: Any?) {
        this.what = what
        this.`object` = `object`
    }

    constructor(what: Int) {
        this.what = what
    }
}
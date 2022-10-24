package com.fingertip.baselib.event_bus


class MessageEvent {
    var what = 0
    var `object`: Any? = null
    //目前只用于标识哪个类发出的事件
    var tag: String? = null

    constructor(what: Int) {
        this.what = what
    }

    constructor(what: Int, `object`: Any?) {
        this.what = what
        this.`object` = `object`
    }

    constructor(what: Int, obj: Any?, tag: String? = null):this(what) {
        this.what = what
        this.`object` = obj
        this.tag = tag
    }
}
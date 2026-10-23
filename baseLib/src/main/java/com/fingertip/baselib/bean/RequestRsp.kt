package com.fingertip.baselib.bean

class RequestRsp<T>: TopData() {
    var code = 0
    var msg: String? = null
    var data: T? = null
}
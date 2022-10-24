package com.fingertip.baselib.bean

class RequestRsp<T>: TopData() {
    var code = 0
    var message: String? = null
    var data: T? = null
}
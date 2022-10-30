package com.fingertip.baselib.bean

class ServerStatusEntity:TopData() {
    var status:Int = 0 //1正常 2强更 3维护
    var updateUrl = "" //如果是强更的话，则返回下载地址
}
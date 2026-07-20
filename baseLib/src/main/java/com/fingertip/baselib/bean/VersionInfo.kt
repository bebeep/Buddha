package com.fingertip.baselib.bean

class VersionInfo: TopData() {

    //1 安卓 2 iOS
    var platType = 0

    //渠道ID
    var channelId = 0

    //应用版本号
    var appVersion: String? = null

    //版本状态
    var versionStatus = 0

    //服务器地址
    var serverAddr: String? = null

    //下载地址
    var downloadUrl: String? = null

    //更新信息
    var updateInfo: String? = null

    //全局参数
    var globalParam: GlobalParam? = null
}
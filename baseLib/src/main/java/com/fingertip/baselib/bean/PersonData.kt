package com.fingertip.baselib.bean



class PersonData : TopData {

    //用户唯一id，不可更改
    var accountId: Int = 0
    //用户昵称
    var userName: String? = ""
    //头像
    var avatar: String? = ""
    //渠道号
    var channelId: Int = 0
    //客户端版本
    var clientVersion: String? = ""
    //金币数
    var coin: Int = 0
    //封面
    var cover: String? = ""
    //设备名称
    var deviceName: String? = ""
    //设备UUID
    var deviceUUID: String? = ""
    //显示的用户id
    var displayAccountId: String? = ""
    //粉丝数
    var followerCount: Int? = 0
    //关注数
    var followingCount: Int? = 0
    //性别0 未知 1 男 2女
    var gender: Int? = 0
    //是否删除
    var isDeleted: Int? = 0
    //最新登录时间
    var latestLoginDateTime: String? = ""
    //最新登录ip
    var latestLoginIp: String? = ""
    //最新会话
    var latestSession: String? = ""
    //登录状态  1登录 2登出
    var loginStatus: Int? = 0
    //心情
    var mood: String? = ""
    //手机号
    var phoneNum: String? = ""
    //平台 1 安卓 2 iOS
    var platform: Int? = 0
    //充值金额
    var rechargeAmount: Int? = 0
    //注册时间
    var registerDateTime: String? = ""
    //注册设备UUID
    var registerDeviceUUID: String? = ""
    //注册ip
    var registerIp: String? = ""
    //角色状态 （1：正常 2：黑名单 )
    var roleStatus: Int? = 0
    //角色类型 （1：普通用户 2：认证大师）
    var roleType: Int? = 0
    //用户经验
    var userExp: Int? = 0
    //用户等级
    var userLevel: Int? = 0



    constructor(name: String, accoutId: Int, avatar: String?) {
        this.userName = name
        this.accountId = accoutId
        this.avatar = avatar
    }


}
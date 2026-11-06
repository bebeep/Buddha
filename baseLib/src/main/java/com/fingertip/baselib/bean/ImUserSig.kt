package com.fingertip.baselib.bean

/**
 * 腾讯云 IM 登录凭证（/account/getImUserSig 返回）
 *
 * userSig 由服务端用应用密钥签发，客户端只负责使用；
 * 过期（onUserSigExpired）时重新调接口拿新的即可。
 */
class ImUserSig : TopData() {
    var userId: String? = null
    var userSig: String? = null
    var sdkAppId = 0
    //相对有效期（秒）
    var expire = 0L
    //绝对过期时间戳（秒）
    var expireAt = 0L
    //官方账号 userID（会话置顶 / 活动消息来源）
    var officialUserId: String? = null
}

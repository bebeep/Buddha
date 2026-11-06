package com.fingertip.baselib.constant


/**
 * SharedPreferences的key
 */
object SPConstant {

    const val SP_ACCOUNT = "SP_ACCOUNT"

    /** 旧版（手机号+密码）遗留 key，仅用于升级时清理，不再读写 */
    const val SP_ACCOUNT_USERNAME = "SP_ACCOUNT_USERNAME"
    const val SP_ACCOUNT_PASSWORD = "SP_ACCOUNT_PASSWORD"

    /** 登录态 session，用于启动时续期。这是海外版唯一的长效登录凭证 */
    const val SP_SESSION = "SP_SESSION"

    /** 上次登录用的邮箱，登出后回填输入框用 */
    const val SP_LAST_EMAIL = "SP_LAST_EMAIL"

    /** 登录方式：1 邮箱 2 Google 3 Apple 0 游客 */
    const val SP_LOGIN_TYPE = "SP_LOGIN_TYPE"

    /** FCM token：设备档案上报 / 清除的定位键，退出登录时先调接口再清 */
    const val SP_FCM_TOKEN = "SP_FCM_TOKEN"
}

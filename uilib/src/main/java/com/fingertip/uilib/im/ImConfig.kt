package com.fingertip.uilib.im

/**
 * 腾讯云 IM 配置
 *
 * 账号体系：IM userID = "u_<accountId>"，与后端 ImServiceImpl 的 USER_ID_PREFIX 一致。
 * 密钥不进客户端：userSig 由服务端 /account/getImUserSig 用应用密钥签发，
 * SDK 回调 onUserSigExpired 时重新调该接口即可（见 ImManager.login）。
 *
 * 官方账号：我们自己定义的一个特殊 accountId（与服务端 buddha.official.accountId 同一个，
 * 即「菩提讲堂」官方号，accountId=32359288）。客户端不需要额外配置下发，
 * 只判断会话/消息来源是不是这个账号。服务端登录接口会带回 officialUserId，
 * 若返回则优先用它（改官方号不用发版）。
 */
object ImConfig {

    const val SDK_APP_ID = 20050404

    /**
     * 官方账号 accountId（特殊标识）。对应 IM userID = "u_32359288"。
     * 服务端通过腾讯云 IM REST API 用该账号下发官方消息 / 活动消息。
     */
    const val OFFICIAL_ACCOUNT_ID = 32359288

    const val USER_ID_PREFIX = "u_"

    /** 服务端下发的官方 userId（登录 IM 时写入），为空时用本地常量兜底 */
    @Volatile
    private var officialUserIdFromServer: String? = null

    fun userIdOf(accountId: Int): String = USER_ID_PREFIX + accountId

    /** 官方账号 IM userID */
    fun officialUserId(): String =
        officialUserIdFromServer?.takeIf { it.isNotBlank() }
            ?: userIdOf(OFFICIAL_ACCOUNT_ID)

    fun isOfficialAccount(accountId: Int): Boolean = accountId == OFFICIAL_ACCOUNT_ID

    /** 服务端下发的官方 id 覆盖本地常量 */
    fun setOfficialUserId(userId: String?) {
        if (!userId.isNullOrBlank()) officialUserIdFromServer = userId
    }

    /** 从 IM userID 反解 accountId；非 "u_<数字>" 结构返回 0 */
    fun accountIdOf(userId: String?): Int {
        val id = userId ?: return 0
        if (!id.startsWith(USER_ID_PREFIX)) return 0
        return id.removePrefix(USER_ID_PREFIX).toIntOrNull() ?: 0
    }
}

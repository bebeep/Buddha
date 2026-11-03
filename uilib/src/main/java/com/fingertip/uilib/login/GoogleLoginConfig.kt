package com.fingertip.uilib.login

/**
 * Google 登录配置
 *
 * SERVER_CLIENT_ID 必须填「Web 应用」类型的 OAuth 2.0 Client ID
 * （Google Cloud Console → API 和服务 → 凭据），不是 Android 类型的那个。
 *
 * 原因：Credential Manager 拿到的 idToken，其 aud 就是这个 Web Client ID。
 * 服务端 third-auth.google.client-ids 里也要包含同一个值，否则 aud 校验不过。
 * Android 类型 Client ID 反而只用于绑定包名 + SHA1，不参与 idToken 签发。
 */
object GoogleLoginConfig {

    /**
     * TODO 上线前替换为真实的 Web Client ID，形如
     * 1234567890-abcdefg.apps.googleusercontent.com
     *
     * 留空时登录页会隐藏 Google 按钮 —— 宁可少一个入口，
     * 也不能让用户点了之后拿到一个一定失败的 idToken。
     */
    const val SERVER_CLIENT_ID = ""

    val isConfigured: Boolean
        get() = SERVER_CLIENT_ID.isNotBlank() && SERVER_CLIENT_ID.endsWith(".apps.googleusercontent.com")
}

package com.fingertip.uilib.push

/**
 * OneSignal 配置
 *
 * APP_ID 在 OneSignal 后台 → Settings → Keys & IDs → OneSignal App ID。
 * Android 端还需要把 FCM 服务账号 JSON 传到 OneSignal（Settings → Push Platforms → Google Android），
 * 否则控制台能建通知但设备收不到 —— 这一步在后台做，代码里没有对应配置。
 */
object PushConfig {

    /**
     * TODO 上线前替换。留空时整个推送链路自动跳过（不初始化、不上报、不申请权限），
     * 不会影响其它功能。
     */
    const val ONESIGNAL_APP_ID = ""

    val isConfigured: Boolean
        get() = ONESIGNAL_APP_ID.isNotBlank() && ONESIGNAL_APP_ID.contains("-")
}

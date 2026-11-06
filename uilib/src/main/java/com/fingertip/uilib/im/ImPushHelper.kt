package com.fingertip.uilib.im

import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.log
import com.tencent.imsdk.v2.V2TIMOfflinePushConfig
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMManager

/**
 * IM 离线推送辅助
 *
 * 原理：App 退后台时调 doBackground 告知 IM 后台「用户不在线」，此后新消息由
 * 腾讯云 IM 后台直接通过厂商通道（FCM）下发系统通知；回前台 doForeground 恢复长连接收消息。
 *
 * 前提（腾讯云控制台一次性配置，代码无需改动）：
 *   IM 控制台 → 离线推送 → 配置 Google FCM 证书，拿到证书 ID（businessID）。
 *   客户端 FCM token 由 DeviceProfile 拉取（firebase-messaging），拿到后调 [registerFcmToken] 上报。
 */
object ImPushHelper {

    private const val TAG = "ImPushHelper"

    //控制台配好 FCM 证书后填这里（证书 ID）。0 = 未配置，跳过上报
    var fcmBusinessId = 9290L

    /** 上报 FCM 离线推送 token（登录成功后调用一次） */
    fun registerFcmToken(token: String) {
        if (token.isBlank()) return
        if (fcmBusinessId <= 0) {
            log(TAG, "FCM token 上报跳过：fcmBusinessId 未配置（ImPushHelper.kt 里填 IM 控制台的证书 ID）")
            return
        }
        if (!ImManager.loggedIn) {
            //不直接上报（SDK 会报 6014 not login），token 已存 SP，登录成功后由 registerSavedToken 补注册
            log(TAG, "FCM token 暂存（IM 尚未登录，登录成功后自动注册）")
            return
        }
        try {
            V2TIMManager.getOfflinePushManager()
                .setOfflinePushConfig(V2TIMOfflinePushConfig(fcmBusinessId, token),
                    object : V2TIMCallback {
                        override fun onSuccess() {
                            log(TAG, "FCM 离线推送 token 上报成功")
                        }

                        override fun onError(code: Int, desc: String?) {
                            log(TAG, "FCM token 上报失败 code=$code desc=$desc")
                        }
                    })
        } catch (e: Exception) {
            log(TAG, "FCM token 上报异常 ${e.message}")
        }
    }

    /**
     * IM 登录成功后补注册离线推送 token。
     * token 由 DeviceProfile 拉到后存进 SP（无论当时是否已登录），
     * 这里统一读取注册，消除「token 先到、登录后到」的竞态。
     */
    fun registerSavedToken() {
        try {
            val token = SPUtils.getInstance(SPConstant.SP_ACCOUNT)
                .getString(SPConstant.SP_FCM_TOKEN)
            if (token.isNotEmpty()) {
                registerFcmToken(token)
            }
        } catch (e: Exception) {
            log(TAG, "registerSavedToken 异常 ${e.message}")
        }
    }

    /** 退后台（UIApplication ActivityLifecycleCallbacks onActivityStopped） */
    fun goBackground(totalUnread: Int = 0) {
        try {
            V2TIMManager.getOfflinePushManager().doBackground(totalUnread, object : V2TIMCallback {
                override fun onSuccess() {
                    log(TAG, "doBackground 成功：此后新消息走腾讯云离线推送（厂商通道）")
                }

                override fun onError(code: Int, desc: String?) {
                    log(TAG, "doBackground 失败 code=$code desc=$desc")
                }
            })
        } catch (e: Exception) {
            log(TAG, "doBackground 异常 ${e.message}")
        }
    }

    /** 回前台 */
    fun goForeground() {
        try {
            V2TIMManager.getOfflinePushManager().doForeground(object : V2TIMCallback {
                override fun onSuccess() {
                    log(TAG, "doForeground 成功：恢复长连接在线收消息")
                }

                override fun onError(code: Int, desc: String?) {
                    log(TAG, "doForeground 失败 code=$code desc=$desc")
                }
            })
        } catch (e: Exception) {
            log(TAG, "doForeground 异常 ${e.message}")
        }
    }
}

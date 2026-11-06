package com.fingertip.uilib.push

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.log
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.uilib.R
import com.fingertip.uilib.im.ImPushHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.TimeZone

/**
 * 设备档案（离线推送 / 定时提醒的数据源）
 *
 * OneSignal 已下线，推送统一走腾讯云 IM：
 *   - 离线推送：FCM token 经 ImPushHelper 上报 IM SDK，App 退后台后由腾讯云通过厂商通道下发
 *   - 提醒类通知（早课/佛历）：服务端按本类上报的时区算本地时间，用官方账号发 IM 消息
 *
 * 职责：
 *   1. Firebase 手动初始化（工程没有 google-services 插件，projectId/apiKey/senderId 在 strings.xml 配）
 *   2. 登录后拉 FCM token，上报服务端设备档案（account_push_record）并同步给 IM SDK
 *   3. 退出登录时按 token 清掉档案记录（IM logout 后离线推送自然停，这里只是清数据）
 *   4. 通知权限申请入口
 */
object DeviceProfile {

    private const val TAG = "DeviceProfile"
    private const val REQ_NOTIFICATION = 6201

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var firebaseReady = false

    /** Application 启动时调用。strings.xml 未配置 Firebase 参数时整段跳过，不影响其它功能 */
    fun initFirebase(context: Context) {
        if (firebaseReady) return
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firebaseReady = true
                return
            }
            val res = context.resources
            val projectId = res.getString(R.string.fcm_project_id)
            val apiKey = res.getString(R.string.fcm_api_key)
            val senderId = res.getString(R.string.fcm_sender_id)
            val appId = res.getString(R.string.fcm_app_id)
            if (projectId.isBlank() || apiKey.isBlank() || senderId.isBlank() || appId.isBlank()) {
                log(TAG, "Firebase 未配置（strings.xml fcm_project_id/fcm_api_key/fcm_sender_id/fcm_app_id），跳过初始化")
                return
            }
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(projectId)
                    .setApiKey(apiKey)
                    //注意是 Firebase App ID（1:xxx:android:xxx），不是 Android 包名
                    .setApplicationId(appId)
                    .setGcmSenderId(senderId)
                    .build()
            )
            firebaseReady = true
            log(TAG, "Firebase 手动初始化完成")
        } catch (e: Exception) {
            log(TAG, "Firebase 初始化失败 ${e.message}")
        }
    }

    /** 登录成功后调用：拉 FCM token → 上报设备档案 → 同一份 token 给 IM SDK 走离线推送 */
    fun report() {
        if (!firebaseReady) {
            log(TAG, "report 跳过：Firebase 未初始化成功（看启动时 initFirebase 的日志找原因）")
            return
        }
        if (GlobalConfig.session.isEmpty()) {
            log(TAG, "report 跳过：登录态 session 为空")
            return
        }
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (token.isNullOrBlank()) {
                        log(TAG, "FCM token 回调成功但内容为空")
                        return@addOnSuccessListener
                    }
                    log(TAG, "FCM token 已获取：${token.take(12)}...")
                    SPUtils.getInstance(SPConstant.SP_ACCOUNT).put(SPConstant.SP_FCM_TOKEN, token)
                    //离线推送：token 交给 IM SDK（退后台后腾讯云用它在厂商通道下发通知）
                    ImPushHelper.registerFcmToken(token)
                    //设备档案：提醒任务用它算「用户的本地现在几点」
                    scope.launch {
                        try {
                            NetManager.getApi().registerPush(
                                request = RequestBodyFactory.deviceProfileBody(
                                    fcmToken = token,
                                    timeZone = TimeZone.getDefault().id,
                                    lang = Locale.getDefault().language
                                )
                            )
                            log(TAG, "设备档案已上报")
                        } catch (e: Exception) {
                            log(TAG, "设备档案上报失败 ${e.message}")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    //最常见的失败原因：设备网络到不了 Google（mtalk.google.com 不通）
                    log(TAG, "FCM token 获取失败 ${e.message}")
                }
        } catch (e: Exception) {
            log(TAG, "获取 FCM token 异常 ${e.message}")
        }
    }

    /** 退出登录 / 注销账号时调用：按 token 清档案。必须先调接口再清本地（接口要带这个 token 定位记录） */
    fun clear() {
        val sp = SPUtils.getInstance(SPConstant.SP_ACCOUNT)
        val token = sp.getString(SPConstant.SP_FCM_TOKEN)
        sp.remove(SPConstant.SP_FCM_TOKEN)
        if (token.isNotEmpty()) {
            scope.launch {
                try {
                    NetManager.getApi().unregisterPush(fcmToken = token)
                    log(TAG, "设备档案已清除")
                } catch (e: Exception) {
                    log(TAG, "清除设备档案失败 ${e.message}")
                }
            }
        }
    }

    /** 通知权限是否已开（提醒页决定要不要显示「通知未开启」引导条） */
    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    /** 登录后申请一次通知权限（冷启动弹窗被拒率高，所以放登录之后） */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasNotificationPermission(activity)) return
        try {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION
            )
        } catch (e: Exception) {
            log(TAG, "申请通知权限失败 ${e.message}")
        }
    }
}

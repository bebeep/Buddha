package com.fingertip.uilib.push

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.log
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.onesignal.OneSignal
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.TimeZone

/**
 * 推送（OneSignal）
 *
 * 关键设计：用 external_id 绑定账号，而不是把 subscriptionId 当作唯一身份。
 * 客户端 OneSignal.login(accountId)，服务端按 external_id 投递，
 * 这样换机、重装、多设备都不用服务端维护映射关系。
 *
 * subscriptionId 仍然上报一份到服务端，只用于后台排查「某台设备到底注册上没有」。
 */
object PushManager {

    private const val TAG = "PushManager"

    /** 订阅 id 异步生成，登录后给它一点时间再去读 */
    private const val FIRST_READ_DELAY = 1500L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var observerAttached = false
    private var boundAccountId = 0

    /**
     * Application 启动时调用。未配置 App ID 时整段跳过，不影响其它功能。
     */
    fun init(context: Context) {
        if (!PushConfig.isConfigured) {
            log(TAG, "未配置 OneSignal App ID，跳过初始化")
            return
        }
        try {
            OneSignal.initWithContext(context.applicationContext, PushConfig.ONESIGNAL_APP_ID)
            log(TAG, "OneSignal 初始化完成")
        } catch (e: Exception) {
            log(TAG, "OneSignal 初始化失败 ${e.message}")
        }
    }

    /**
     * 登录成功后调用。
     */
    fun bind(accountId: Int) {
        if (!PushConfig.isConfigured || accountId <= 0) return

        boundAccountId = accountId
        // external_id 用 accountId 字符串，服务端 include_aliases 直接填它
        OneSignal.login(accountId.toString())
        attachObserver()

        // 权限弹窗放在登录之后：冷启动就弹通知权限，被拒率明显更高。
        // requestPermission 是 suspend，必须进协程（OneSignal 内部会自己切到主线程弹窗）
        mainScope.launch {
            try {
                OneSignal.Notifications.requestPermission(true)
            } catch (e: Exception) {
                log(TAG, "申请通知权限失败 ${e.message}")
            }
        }

        // 权限之前就授权过的设备不会再有变化回调，这里主动读一次兜底
        mainHandler.postDelayed({ readAndReport() }, FIRST_READ_DELAY)
    }

    /**
     * 退出登录 / 注销账号时调用。
     *
     * 顺序很重要：必须先用当前 session 调完解绑接口，再 OneSignal.logout()。
     * 反过来的话解绑请求带的是已失效的 session，服务端会拒，旧账号仍会收到推送。
     */
    fun unbind() {
        val sp = SPUtils.getInstance(SPConstant.SP_ACCOUNT)
        val subscriptionId = sp.getString(SPConstant.SP_PUSH_SUBSCRIPTION_ID)
        sp.remove(SPConstant.SP_PUSH_SUBSCRIPTION_ID)
        boundAccountId = 0

        if (subscriptionId.isNotEmpty()) {
            scope.launch {
                try {
                    NetManager.getApi().unregisterPush(subscriptionId = subscriptionId)
                    log(TAG, "推送订阅已解绑 $subscriptionId")
                } catch (e: Exception) {
                    log(TAG, "解绑推送订阅失败 ${e.message}")
                }
            }
        }

        if (PushConfig.isConfigured) {
            OneSignal.logout()
        }
    }

    /** 账号级开关（设置页用） */
    fun setEnabled(enabled: Boolean) {
        if (GlobalConfig.session.isEmpty()) return
        scope.launch {
            try {
                NetManager.getApi().switchPush(enabled = if (enabled) 1 else 0)
            } catch (e: Exception) {
                log(TAG, "切换推送开关失败 ${e.message}")
            }
        }
    }

    /**
     * 通知权限是否已开。提醒页用它决定要不要显示「通知未开启」引导条——
     * 权限没开时用户配了规则也收不到，不提示等于骗人。
     */
    fun hasPermission(): Boolean {
        if (!PushConfig.isConfigured) return false
        return try {
            OneSignal.Notifications.permission
        } catch (e: Exception) {
            log(TAG, "读取通知权限失败 ${e.message}")
            false
        }
    }

    /**
     * 再申请一次权限（用户点引导条时调）。
     * 已永久拒绝的系统不会再弹窗，会直接把用户送到系统设置页。
     */
    fun requestPermission(onResult: ((Boolean) -> Unit)? = null) {
        if (!PushConfig.isConfigured) {
            onResult?.invoke(false)
            return
        }
        mainScope.launch {
            try {
                val granted = OneSignal.Notifications.requestPermission(true)
                onResult?.invoke(granted)
            } catch (e: Exception) {
                log(TAG, "申请通知权限失败 ${e.message}")
                onResult?.invoke(false)
            }
        }
    }

    private fun attachObserver() {
        if (observerAttached) return
        observerAttached = true
        try {
            OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
                override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                    val id = state.current.id
                    if (!id.isNullOrEmpty()) {
                        reportSubscription(id, state.current.token)
                    }
                }
            })
        } catch (e: Exception) {
            log(TAG, "订阅监听失败 ${e.message}")
        }
    }

    private fun readAndReport() {
        try {
            val id = OneSignal.User.pushSubscription.id
            if (!id.isNullOrEmpty()) {
                reportSubscription(id, OneSignal.User.pushSubscription.token)
            } else {
                log(TAG, "订阅 id 尚未生成，等待回调")
            }
        } catch (e: Exception) {
            log(TAG, "读取订阅 id 失败 ${e.message}")
        }
    }

    private fun reportSubscription(subscriptionId: String, token: String?) {
        if (boundAccountId <= 0) return

        SPUtils.getInstance(SPConstant.SP_ACCOUNT)
            .put(SPConstant.SP_PUSH_SUBSCRIPTION_ID, subscriptionId)

        val session = GlobalConfig.session
        if (session.isEmpty()) return

        scope.launch {
            try {
                NetManager.getApi().registerPush(
                    session = session,
                    request = RequestBodyFactory.pushRegisterBody(
                        subscriptionId = subscriptionId,
                        pushToken = token ?: "",
                        // 时区必须上报：早课提醒按用户本地 7 点推，不是服务器 7 点
                        timeZone = TimeZone.getDefault().id,
                        lang = Locale.getDefault().language
                    )
                )
                log(TAG, "推送订阅已上报 $subscriptionId")
            } catch (e: Exception) {
                log(TAG, "推送订阅上报失败 ${e.message}")
            }
        }
    }
}

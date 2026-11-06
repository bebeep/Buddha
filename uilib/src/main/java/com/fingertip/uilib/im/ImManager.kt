package com.fingertip.uilib.im

import android.content.Context
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.event_bus.EventBusProxy
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.net.NetManager
import com.tencent.imsdk.v2.V2TIMAdvancedMsgListener
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMConversationListener
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMSDKConfig
import com.tencent.imsdk.v2.V2TIMSDKListener
import com.tencent.imsdk.v2.V2TIMValueCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 腾讯云 IM 管理器
 *
 * 职责：
 * 1. SDK 初始化（Application 启动时，不依赖登录态）
 * 2. 登录/登出：accountId → "u_<accountId>"，userSig 由服务端签发
 *    （/account/getImUserSig，应用密钥不出服务端；sig 过期回调里会重新拉一次）
 * 3. 全局消息/会话监听：未读角标（EventBus 发给 MainFragment）、
 *    前台新消息本地通知（离线走腾讯云厂商通道推送，见 ImPushHelper）
 *
 * 换设备消息同步：腾讯云 IM 的会话与历史消息都在云端，
 * 新设备登录后 SDK 自动同步会话列表与最近消息（漫游），聊天页历史用
 * getMessageList 云端拉取，无需客户端自己做迁移。
 */
object ImManager {

    private const val TAG = "ImManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    //登录中标记：避免启动 + sig 过期回调同时打两次登录
    @Volatile
    private var loggingIn = false

    var inited = false
        private set

    var loggedIn = false
        private set

    //当前正在查看的会话对象 id（null=不在聊天页）。收消息时用它抑制本会话的重复通知
    var activePeerId: String? = null

    fun init(context: Context) {
        if (inited) return
        try {
            val config = V2TIMSDKConfig().apply {
                setLogLevel(V2TIMSDKConfig.V2TIM_LOG_ERROR)
            }
            val ok = V2TIMManager.getInstance().initSDK(
                context.applicationContext,
                ImConfig.SDK_APP_ID,
                config,
                object : V2TIMSDKListener() {
                    override fun onConnecting() {}
                    override fun onConnectSuccess() {}
                    override fun onConnectFailed(code: Int, error: String?) {
                        log(TAG, "IM 连接失败 code=$code error=$error")
                    }

                    override fun onKickedOffline() {
                        //被其它设备挤下线（默认单设备登录）：换设备后旧设备即走这里，
                        //新设备登录成功自动同步，本地只需标记掉线
                        log(TAG, "被其它设备挤下线")
                        loggedIn = false
                    }

                    override fun onUserSigExpired() {
                        //sig 过期：向服务端重新要一个再登，静默恢复
                        log(TAG, "userSig 已过期，重新登录")
                        if (GlobalConfig.userData != null) {
                            login(GlobalConfig.userData?.accountId ?: 0)
                        }
                    }
                })
            inited = ok
            log(TAG, "IM init ok=$ok")

            V2TIMManager.getMessageManager().addAdvancedMsgListener(msgListener)
            V2TIMManager.getConversationManager().setConversationListener(convListener)
        } catch (e: Exception) {
            log(TAG, "IM init 异常 ${e.message}")
        }
    }

    /** 登录成功/冷启动续期成功后调用 */
    fun login(accountId: Int) {
        if (!inited || accountId <= 0) return
        if (loggingIn) return
        loggingIn = true
        val userId = ImConfig.userIdOf(accountId)
        scope.launch {
            //密钥只在服务端：先拿签名再登 SDK，失败就静默放弃（下次进前台/重启会重试）
            val userSig = fetchUserSig()
            withContext(Dispatchers.Main) {
                loggingIn = false
                if (userSig.isNullOrBlank()) {
                    log(TAG, "拿不到 userSig，IM 暂不登录 $userId")
                    return@withContext
                }
                V2TIMManager.getInstance().login(userId, userSig,
                    object : V2TIMCallback {
                        override fun onSuccess() {
                            log(TAG, "IM 登录成功 $userId")
                            loggedIn = true
                            //登录成功即会拉一次未读，角标立刻就位
                            refreshUnread()
                            //离线推送 token 注册必须等登录完成（否则 SDK 报 6014 not login）：
                            //DeviceProfile 拿到 token 的时机可能早于登录，这里统一补注册
                            ImPushHelper.registerSavedToken()
                        }

                        override fun onError(code: Int, desc: String?) {
                            log(TAG, "IM 登录失败 code=$code desc=$desc")
                        }
                    })
            }
        }
    }

    /**
     * 向服务端要当前账号的 userSig。
     * 服务端用应用密钥签名（TLSSigAPIv2），客户端只持有签名结果，不内置 secretKey。
     */
    private suspend fun fetchUserSig(): String? = try {
        val rsp = NetManager.getApi().getImUserSig()
        val data = rsp.data
        val userSig = data?.userSig
        if (rsp.code != 0 || userSig.isNullOrBlank()) {
            log(TAG, "拉取 userSig 失败 code=${rsp.code} msg=${rsp.msg}")
            null
        } else {
            //服务端可能调整官方账号，返回了就以此为准（本地常量兜底）
            ImConfig.setOfficialUserId(data?.officialUserId)
            userSig
        }
    } catch (e: Exception) {
        log(TAG, "拉取 userSig 异常 ${e.message}")
        null
    }

    /** 退出登录 / 注销账号时调用 */
    fun logout() {
        if (!loggedIn) return
        loggedIn = false
        activePeerId = null
        try {
            V2TIMManager.getInstance().logout(null)
        } catch (e: Exception) {
            log(TAG, "IM logout 异常 ${e.message}")
        }
    }

    private fun refreshUnread() {
        if (!loggedIn) return
        try {
            V2TIMManager.getConversationManager()
                .getTotalUnreadMessageCount(object : V2TIMValueCallback<Long> {
                    override fun onSuccess(t: Long) {
                        EventBusProxy.post(MessageEvent(EventConstant.EVENT_IM_UNREAD, t))
                    }

                    override fun onError(code: Int, desc: String?) {}
                })
        } catch (e: Exception) {
            log(TAG, "拉取未读异常 ${e.message}")
        }
    }

    private val msgListener = object : V2TIMAdvancedMsgListener() {

        override fun onRecvNewMessage(msg: V2TIMMessage) {
            val sender = msg.sender ?: return
            //未读总数刷新交给 onTotalUnreadMessageCountChanged；这里只负责增量分发
            EventBusProxy.post(MessageEvent(EventConstant.EVENT_IM_NEW_MESSAGE, msg))
            //前台且不在该会话 → 本地通知；后台时 SDK 已把应用状态同步给 IM 后台走厂商通道
            if (sender != activePeerId) {
                ImNotification.notifyNewMessage(msg)
            }
        }
    }

    private val convListener = object : V2TIMConversationListener() {

        override fun onTotalUnreadMessageCountChanged(unreadCount: Long) {
            EventBusProxy.post(MessageEvent(EventConstant.EVENT_IM_UNREAD, unreadCount))
        }
    }
}

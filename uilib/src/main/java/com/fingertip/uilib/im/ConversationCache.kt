package com.fingertip.uilib.im

import android.content.Context
import com.fingertip.baselib.constant.GlobalConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * 会话列表本地快照。
 *
 * 腾讯 IM SDK 的会话库虽然也在本地，但冷启动要先登录+同步才回调 getConversationList，
 * 这期间列表是空的（空屏）。这里把上一次拉到的列表按账号缓存到 SharedPreferences：
 * 进页先秒出缓存，SDK 回调回来再覆盖并回写，空屏基本消失。
 */
object ConversationCache {

    private const val SP_NAME = "im_conversation_cache"
    private const val KEEP_MAX = 100

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    //按账号隔离：换账号不串数据（未登录返回空）
    private fun key(): String? {
        val accountId = GlobalConfig.userData?.accountId ?: 0
        return if (accountId > 0) "conv_$accountId" else null
    }

    fun read(context: Context): List<ConversationItem> {
        val k = key() ?: return emptyList()
        val raw = prefs(context).getString(k, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ConversationItem(
                    peerId = o.optString("peerId"),
                    name = o.optString("name"),
                    avatar = o.optString("avatar"),
                    summary = o.optString("summary"),
                    timeText = o.optString("timeText"),
                    unread = o.optInt("unread"),
                    isOfficial = o.optBoolean("isOfficial"),
                    lastTimestamp = o.optLong("lastTimestamp")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, list: List<ConversationItem>) {
        val k = key() ?: return
        try {
            val arr = JSONArray()
            list.take(KEEP_MAX).forEach { c ->
                arr.put(
                    JSONObject().apply {
                        put("peerId", c.peerId)
                        put("name", c.name)
                        put("avatar", c.avatar)
                        put("summary", c.summary)
                        put("timeText", c.timeText)
                        put("unread", c.unread)
                        put("isOfficial", c.isOfficial)
                        put("lastTimestamp", c.lastTimestamp)
                    }
                )
            }
            prefs(context).edit().putString(k, arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    /** 退出登录/切换账号时清掉，避免下一个账号看到上个账号的会话 */
    fun clear(context: Context) {
        try {
            prefs(context).edit().clear().apply()
        } catch (_: Exception) {
        }
    }
}

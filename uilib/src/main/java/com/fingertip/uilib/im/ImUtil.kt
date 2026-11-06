package com.fingertip.uilib.im

import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMManager

/**
 * 会话辅助：conversationId 解析、置顶排序
 */
object ImUtil {

    /**
     * conversationID 格式："c2c_<userId>" / "group_<groupId>"
     */
    fun userIdOfConversation(conv: V2TIMConversation): String {
        val id = conv.conversationID ?: return ""
        return if (id.startsWith("c2c_")) id.removePrefix("c2c_") else id
    }

    /**
     * 会话排序：官方账号永远第一，其余按最后一条消息时间降序
     */
    fun sortConversations(list: List<V2TIMConversation>): List<V2TIMConversation> {
        val official = ImConfig.officialUserId()
        return list.sortedWith(
            Comparator { a, b ->
                val aOff = userIdOfConversation(a) == official
                val bOff = userIdOfConversation(b) == official
                when {
                    aOff && !bOff -> -1
                    !aOff && bOff -> 1
                    else -> {
                        val at = a.lastMessage?.timestamp ?: 0
                        val bt = b.lastMessage?.timestamp ?: 0
                        bt.compareTo(at)
                    }
                }
            })
    }

    /**
     * 云端置顶官方会话（把 conversation 的 isPinned 置为 true）。
     * 客户端排序已经保证官方在前，这里双保险：即使会话很多、本地排序失效
     * （例如腾讯云后台直接下发置顶列表），官方也稳居顶部。
     */
    fun pinOfficialConversation() {
        val official = ImConfig.officialUserId()
        try {
            V2TIMManager.getConversationManager().pinConversation(
                "c2c_$official", true, null
            )
        } catch (_: Exception) {
        }
    }
}

package com.fingertip.uilib.im

/**
 * 会话列表的展示模型。
 *
 * 为什么不用 V2TIMConversation 直接上屏：SDK 对象不能本地持久化，
 * 冷启动时列表要等 SDK 登录+同步完成才有数据 → 空屏。抽成本地可序列化的模型后，
 * 上一轮的列表可以缓存到本地，进页立刻渲染，SDK 回调回来再覆盖（见 ConversationCache）。
 */
data class ConversationItem(
    //对方 userId("u_123" 或官方账号 id)
    val peerId: String = "",
    val name: String = "",
    val avatar: String = "",
    //最后一条消息摘要（已格式化）
    val summary: String = "",
    //列表右侧时间文案（已格式化）
    val timeText: String = "",
    val unread: Int = 0,
    val isOfficial: Boolean = false,
    //排序用：最后消息时间戳
    val lastTimestamp: Long = 0
)

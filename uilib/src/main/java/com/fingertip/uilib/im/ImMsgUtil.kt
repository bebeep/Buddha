package com.fingertip.uilib.im

import com.tencent.imsdk.v2.V2TIMMessage
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 消息工具：类型摘要、官方活动消息解析、媒体消息（OSS 自有存储）解析
 *
 * 官方活动消息 = 自定义消息，data 为 JSON：
 *   {"bizType":"activity","title":"...","desc":"...","url":"https://...","cover":"https://..."}
 * 服务端通过腾讯云 IM REST API 用官方账号下发，客户端解析后渲染成活动卡。
 *
 * 媒体消息 = 自定义消息，文件存我们自己的 OSS，IM 只传 JSON：
 *   {"bizType":"media","mediaType":"image|video|sound","url":"https://oss...",
 *    "width":1080,"height":1920,"duration":12,"thumb":"https://oss.../cover.jpg"}
 * thumb 仅视频封面；width/height 仅图片；duration 秒（视频/语音）。
 */
object ImMsgUtil {

    /** 会话列表摘要 / 通知内容 */
    fun summaryOf(msg: V2TIMMessage): String = when (msg.elemType) {
        V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> msg.textElem?.text ?: ""
        V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE -> "[图片]"
        V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO -> "[视频]"
        V2TIMMessage.V2TIM_ELEM_TYPE_SOUND -> "[语音]"
        V2TIMMessage.V2TIM_ELEM_TYPE_CUSTOM -> parseMedia(msg)?.let { "[${it.label}]" }
            ?: parseActivity(msg)?.let { "[活动] ${it.title}" } ?: "[消息]"
        else -> "[消息]"
    }

    /**
     * 解析官方活动消息。bizType != activity 的自定义消息返回 null，
     * 按普通自定义消息兜底展示
     */
    fun parseActivity(msg: V2TIMMessage): ActivityMsg? {
        val data = msg.customElem?.data ?: return null
        return parseActivityJson(String(data))
    }

    fun parseActivityJson(json: String): ActivityMsg? = try {
        val obj = JSONObject(json)
        if (obj.optString("bizType") == "activity" && obj.optString("url").isNotBlank()) {
            ActivityMsg(
                title = obj.optString("title"),
                desc = obj.optString("desc"),
                url = obj.optString("url"),
                cover = obj.optString("cover")
            )
        } else null
    } catch (_: Exception) {
        null
    }

    fun buildActivityJson(msg: ActivityMsg): String = JSONObject().apply {
        put("bizType", "activity")
        put("title", msg.title)
        put("desc", msg.desc)
        put("url", msg.url)
        put("cover", msg.cover)
    }.toString()

    // ==================== 媒体消息（OSS 自有存储） ====================

    fun parseMedia(msg: V2TIMMessage): MediaMsg? {
        val data = msg.customElem?.data ?: return null
        return parseMediaJson(String(data))
    }

    fun parseMediaJson(json: String): MediaMsg? = try {
        val obj = JSONObject(json)
        if (obj.optString("bizType") == "media" && obj.optString("url").isNotBlank()) {
            MediaMsg(
                mediaType = obj.optString("mediaType"),
                url = obj.optString("url"),
                width = obj.optInt("width", 0),
                height = obj.optInt("height", 0),
                duration = obj.optInt("duration", 0),
                thumb = obj.optString("thumb")
            )
        } else null
    } catch (_: Exception) {
        null
    }

    fun buildMediaJson(msg: MediaMsg): String = JSONObject().apply {
        put("bizType", "media")
        put("mediaType", msg.mediaType)
        put("url", msg.url)
        if (msg.width > 0) put("width", msg.width)
        if (msg.height > 0) put("height", msg.height)
        if (msg.duration > 0) put("duration", msg.duration)
        if (msg.thumb.isNotBlank()) put("thumb", msg.thumb)
    }.toString()

    /** 判断消息是否来自官方账号（官方账号 = 特定的 accountId，见 ImConfig.OFFICIAL_ACCOUNT_ID） */
    fun isOfficial(msg: V2TIMMessage): Boolean = msg.sender == ImConfig.officialUserId()

    fun isOfficialConv(convUserId: String): Boolean = convUserId == ImConfig.officialUserId()

    /** 按 accountId 判断（服务端下发活动消息时也用这个口径） */
    fun isOfficialAccount(accountId: Int): Boolean = ImConfig.isOfficialAccount(accountId)

    fun timeOf(timestampSeconds: Long): String {
        if (timestampSeconds <= 0) return ""
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampSeconds * 1000))
    }

    /** 今天/昨天/日期 的会话时间戳 */
    fun conversationTime(timestampSeconds: Long): String {
        if (timestampSeconds <= 0) return ""
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val todayStart = cal.timeInMillis / 1000
        return when {
            timestampSeconds >= todayStart -> timeOf(timestampSeconds)
            timestampSeconds >= todayStart - 86400 -> "昨天"
            else -> SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timestampSeconds * 1000))
        }
    }

    fun durationOf(seconds: Int): String =
        if (seconds >= 60) "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}" else "${seconds}s"
}

/**
 * 官方活动消息实体
 */
data class ActivityMsg(
    val title: String,
    val desc: String,
    val url: String,
    val cover: String
)

/**
 * 媒体消息实体（文件在我们自己的 OSS 上，IM 只传 JSON）
 */
data class MediaMsg(
    val mediaType: String,  // image / video / sound
    val url: String,        // OSS 访问 URL
    val width: Int = 0,     // 图片宽
    val height: Int = 0,    // 图片高
    val duration: Int = 0,  // 秒（视频/语音）
    val thumb: String = ""  // 视频封面 URL
) {
    val label: String
        get() = when (mediaType) {
            "image" -> "图片"
            "video" -> "视频"
            "sound" -> "语音"
            else -> "消息"
        }
}

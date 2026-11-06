package com.fingertip.uilib.adapter

import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.top.TopRcAdapter.TopRcViewHolder
import com.fingertip.baselib.util.loadImg
import com.lzlz.toplib.extention.toPx
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemChatMsgBinding
import com.fingertip.uilib.im.ActivityMsg
import com.fingertip.uilib.im.ImConfig
import com.fingertip.uilib.im.ImMsgUtil
import com.fingertip.baselib.R as BaseR
import com.tencent.imsdk.v2.V2TIMImageElem
import com.tencent.imsdk.v2.V2TIMMessage
import android.media.MediaPlayer

/**
 * 聊天消息列表。一条 item 承载全部类型（按 elemType 显隐），
 * 我方/对方靠头像位置 + 气泡 gravity 对齐。
 *
 * 注意：视频/语音的云端 URL 是异步 API（getVideoUrl/getSnapshotUrl/getUrl），
 * 本地 path 只有发送方有，渲染时 path 优先、URL 回调兜底。
 */
class ChatMessageAdapter(context: android.content.Context) :
    TopRcAdapter<V2TIMMessage, ChatMessageAdapter.Holder>(context) {

    //对方头像（进聊天页传入）
    var peerAvatar: String = ""

    //图片预览回调（复用 PhotoPicker 的全屏预览页）
    var onPreviewImage: ((url: String) -> Unit)? = null

    //活动卡点击（跳 H5）
    var onOpenActivity: ((ActivityMsg) -> Unit)? = null

    class Holder(view: View) : TopRcViewHolder(view) {
        lateinit var b: ItemChatMsgBinding
    }

    override fun initLayoutId(viewType: Int) = R.layout.item_chat_msg

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder {
        val v = android.view.LayoutInflater.from(context).inflate(R.layout.item_chat_msg, parent, false)
        return Holder(v).apply {
            b = ItemChatMsgBinding.bind(v)
            //头像强制圆形裁切：占位图/兜底图不走 Glide transform，直接在 View 层裁
            val clip = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            b.ivAvatarLeft.outlineProvider = clip
            b.ivAvatarRight.outlineProvider = clip
            b.ivAvatarLeft.clipToOutline = true
            b.ivAvatarRight.clipToOutline = true
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val msg = get(position) ?: return
        val b = holder.b
        val isMine = msg.isSelf

        //头像：我方在右、对方在左，圆形裁切；官方账号兜底本地莲花头像
        val official = ImMsgUtil.isOfficial(msg)
        val avatarView = if (isMine) b.ivAvatarRight else b.ivAvatarLeft
        b.ivAvatarLeft.visibility = if (isMine) View.GONE else View.VISIBLE
        b.ivAvatarRight.visibility = if (isMine) View.VISIBLE else View.GONE

        val holderIcon = if (official) R.drawable.icon_im_official_avatar else BaseR.mipmap.icon_default_head
        val avatarUrl = when {
            official -> null
            isMine -> GlobalConfig.userData?.avatar
            else -> peerAvatar.takeIf { it.isNotBlank() }
        }
        avatarView.loadImg(avatarUrl, holder = holderIcon, transform = CircleCrop())

        //时间：相邻消息间隔超 5 分钟才显示
        val prev = get(position - 1)
        val showTime = prev == null || (msg.timestamp - prev.timestamp) > 300
        b.tvTime.visibility = if (showTime) View.VISIBLE else View.GONE
        b.tvTime.text = ImMsgUtil.conversationTime(msg.timestamp)

        //气泡对齐与配色
        val lp = b.llBubble.layoutParams as android.widget.LinearLayout.LayoutParams
        lp.gravity = if (isMine) Gravity.END else Gravity.START
        b.llBubble.layoutParams = lp
        b.llBubble.setBackgroundResource(
            if (isMine) R.drawable.bg_im_bubble_mine else R.drawable.bg_im_bubble_other
        )

        //默认全隐藏
        b.tvText.visibility = View.GONE
        b.ivImage.visibility = View.GONE
        b.flVideo.visibility = View.GONE
        b.llSound.visibility = View.GONE
        b.llActivity.visibility = View.GONE

        when (msg.elemType) {
            V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> {
                b.tvText.visibility = View.VISIBLE
                b.tvText.text = msg.textElem?.text ?: ""
            }

            V2TIMMessage.V2TIM_ELEM_TYPE_IMAGE -> renderImage(b, msg)
            V2TIMMessage.V2TIM_ELEM_TYPE_VIDEO -> renderVideo(b, msg)
            V2TIMMessage.V2TIM_ELEM_TYPE_SOUND -> renderSound(b, msg)
            V2TIMMessage.V2TIM_ELEM_TYPE_CUSTOM -> {
                val media = ImMsgUtil.parseMedia(msg)
                if (media != null) renderMedia(b, media) else {
                    val activity = ImMsgUtil.parseActivity(msg)
                    if (activity != null) renderActivity(b, activity) else {
                        //非活动/媒体自定义消息按文本兜底
                        b.tvText.visibility = View.VISIBLE
                        b.tvText.text = ImMsgUtil.summaryOf(msg)
                    }
                }
            }

            else -> {
                b.tvText.visibility = View.VISIBLE
                b.tvText.text = ImMsgUtil.summaryOf(msg)
            }
        }
    }

    private fun renderImage(b: ItemChatMsgBinding, msg: V2TIMMessage) {
        b.ivImage.visibility = View.VISIBLE
        val images = msg.imageElem?.imageList
        //发送方本地有原图 path，接收方用云端 url（LARGE 平衡清晰度与流量）
        val image = images?.firstOrNull { it.type == V2TIMImageElem.V2TIM_IMAGE_TYPE_LARGE }
            ?: images?.firstOrNull()
        val url = image?.url
        //尺寸按 220dp 上限等比缩放（注意 loadImg 的 width 参数是 px，直接传 dp 值会小得离谱）
        val maxPx = 220.toPx()
        val wPx = maxPx
        val hPx = if ((image?.width ?: 0) > 0 && image != null) {
            (maxPx * image.height / image.width).coerceAtLeast(40.toPx())
        } else maxPx
        b.ivImage.layoutParams = b.ivImage.layoutParams.apply { width = wPx; height = hPx }
        b.ivImage.loadImg(url, holder = BaseR.mipmap.icon_default_img, width = wPx, height = hPx)
        b.ivImage.setOnClickListener {
            image?.url?.let { onPreviewImage?.invoke(it) }
        }
    }

    /**
     * 自有 OSS 媒体消息（IM 只传 JSON）：
     * image → 按宽高比渲染；video → 封面+播放；sound → 播放键+时长
     * OSS 桶是私有的：渲染/播放前先经服务端换签名 URL（ImMediaUrl 缓存）
     */
    private fun renderMedia(b: ItemChatMsgBinding, media: com.fingertip.uilib.im.MediaMsg) {
        when (media.mediaType) {
            "image" -> {
                b.ivImage.visibility = View.VISIBLE
                val maxPx = 220.toPx()
                val wPx = maxPx
                val hPx = if (media.width > 0 && media.height > 0) {
                    (maxPx * media.height / media.width).coerceAtLeast(40.toPx())
                } else maxPx
                b.ivImage.layoutParams = b.ivImage.layoutParams.apply { width = wPx; height = hPx }
                com.fingertip.uilib.im.ImMediaUrl.resolve(media.url) { url ->
                    b.ivImage.loadImg(url, holder = BaseR.mipmap.icon_default_img, width = wPx, height = hPx)
                }
                b.ivImage.setOnClickListener {
                    //签名 URL 有缓存，这里基本同步返回
                    com.fingertip.uilib.im.ImMediaUrl.resolve(media.url) { url ->
                        onPreviewImage?.invoke(url)
                    }
                }
            }

            "video" -> {
                b.flVideo.visibility = View.VISIBLE
                if (media.thumb.isNotBlank()) {
                    com.fingertip.uilib.im.ImMediaUrl.resolve(media.thumb) { url ->
                        b.ivVideoCover.loadImg(url, holder = BaseR.mipmap.icon_default_img)
                    }
                } else {
                    b.ivVideoCover.loadImg(null, holder = BaseR.mipmap.icon_default_img)
                }
                b.flVideo.setOnClickListener {
                    com.fingertip.uilib.im.ImMediaUrl.resolve(media.url) { url -> openPlayer(url) }
                }
            }

            "sound" -> {
                b.llSound.visibility = View.VISIBLE
                b.tvSoundDuration.text = ImMsgUtil.durationOf(media.duration)
                b.llSound.setOnClickListener {
                    com.fingertip.uilib.im.ImMediaUrl.resolve(media.url) { url -> playUrl(url) }
                }
            }
        }
    }

    private fun renderVideo(b: ItemChatMsgBinding, msg: V2TIMMessage) {
        b.flVideo.visibility = View.VISIBLE
        val elem = msg.videoElem ?: return
        //封面：发送方有本地 snapshotPath，接收方异步取云端 url
        if (!elem.snapshotPath.isNullOrBlank()) {
            b.ivVideoCover.loadImg(elem.snapshotPath, holder = BaseR.mipmap.icon_default_img)
        } else {
            try {
                elem.getSnapshotUrl(object : com.tencent.imsdk.v2.V2TIMValueCallback<String> {
                    override fun onSuccess(t: String?) {
                        b.ivVideoCover.loadImg(t, holder = BaseR.mipmap.icon_default_img)
                    }

                    override fun onError(code: Int, desc: String?) {}
                })
            } catch (_: Exception) {
            }
        }
        b.flVideo.setOnClickListener { playVideo(elem) }
    }

    private fun playVideo(elem: com.tencent.imsdk.v2.V2TIMVideoElem) {
        //发送方直接播本地文件；接收方先异步拿云端地址
        if (!elem.videoPath.isNullOrBlank()) {
            openPlayer(elem.videoPath)
            return
        }
        try {
            elem.getVideoUrl(object : com.tencent.imsdk.v2.V2TIMValueCallback<String> {
                override fun onSuccess(t: String?) {
                    if (!t.isNullOrBlank()) openPlayer(t)
                }

                override fun onError(code: Int, desc: String?) {
                    Toast.makeText(context, context.getString(R.string.im_chat_no_player), Toast.LENGTH_SHORT).show()
                }
            })
        } catch (_: Exception) {
        }
    }

    private fun openPlayer(urlOrPath: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(android.net.Uri.parse(urlOrPath), "video/*")
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.im_chat_no_player), Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderSound(b: ItemChatMsgBinding, msg: V2TIMMessage) {
        b.llSound.visibility = View.VISIBLE
        val elem = msg.soundElem ?: return
        b.tvSoundDuration.text = ImMsgUtil.durationOf(elem.duration)
        b.llSound.setOnClickListener { playSound(elem) }
    }

    private fun renderActivity(b: ItemChatMsgBinding, activity: ActivityMsg) {
        b.llActivity.visibility = View.VISIBLE
        b.tvActivityTitle.text = activity.title
        b.tvActivityDesc.text = activity.desc
        if (activity.cover.isNotBlank()) {
            b.ivActivityCover.visibility = View.VISIBLE
            b.ivActivityCover.loadImg(activity.cover, holder = BaseR.mipmap.icon_default_img)
        } else {
            b.ivActivityCover.visibility = View.GONE
        }
        b.tvActivityGo.setOnClickListener { onOpenActivity?.invoke(activity) }
    }

    // ==================== 语音播放 ====================

    private var player: MediaPlayer? = null

    private fun playSound(elem: com.tencent.imsdk.v2.V2TIMSoundElem) {
        try {
            //再次点击 = 停止
            if (player?.isPlaying == true) {
                player?.stop()
                player?.release()
                player = null
                return
            }
            val source = elem.path
            if (!source.isNullOrBlank()) {
                startPlayer(source)
            } else {
                elem.getUrl(object : com.tencent.imsdk.v2.V2TIMValueCallback<String> {
                    override fun onSuccess(t: String?) {
                        if (!t.isNullOrBlank()) startPlayer(t)
                    }

                    override fun onError(code: Int, desc: String?) {
                        Toast.makeText(context, context.getString(R.string.im_chat_play_fail), Toast.LENGTH_SHORT).show()
                    }
                })
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.im_chat_play_fail), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPlayer(urlOrPath: String) {
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(urlOrPath)
            prepareAsync()
            setOnPreparedListener { it.start() }
            setOnCompletionListener { it.release() }
        }
    }

    /** 播放任意 URL/本地路径（OSS 媒体消息用） */
    private fun playUrl(urlOrPath: String) {
        try {
            startPlayer(urlOrPath)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.im_chat_play_fail), Toast.LENGTH_SHORT).show()
        }
    }
}

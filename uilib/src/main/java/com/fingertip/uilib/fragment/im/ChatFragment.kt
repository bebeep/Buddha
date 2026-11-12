package com.fingertip.uilib.fragment.im

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.ChatMessageAdapter
import com.fingertip.uilib.databinding.FragmentChatBinding
import com.fingertip.uilib.im.ImManager
import com.fingertip.uilib.im.MediaMsg
import com.fingertip.uilib.im.ImMsgUtil
import com.fingertip.uilib.im.VoiceRecorder
import com.fingertip.baselib.util.PicUtils
import com.fingertip.uilib.viewmodel.MainVM
import com.photo.picker.GalleryPickerHelper
import com.photo.picker.MediaData
import com.photo.picker.MediaType
import com.photo.picker.callback.MediaResultCallback
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMValueCallback
import org.greenrobot.eventbus.Subscribe
import androidx.core.view.updatePadding
import kotlinx.coroutines.launch

/**
 * 单聊页
 *
 * 关键设计：
 * - 历史消息首屏先读 SDK 本地库（即时渲染，避免从会话列表进来的空白页），
 *   再云端同步漫游；滚动到顶继续向前翻页，一页 20 条。
 * - 发送：文本走 sendC2C 语义（createTextMessage + sendMessage）；
 *   图片/视频/语音文件统一上传到我们自己的 OSS（PicUtils + STS），
 *   IM 里只发自定义 JSON 消息（bizType=media），不占腾讯 COS。
 * - 官方账号的自定义活动消息在 Adapter 里渲染成活动卡，点击跳 CommonWebViewActivity。
 * - 进页即把会话标已读，未读角标随之清零（convListener 全局联动）。
 */
class ChatFragment : TopPmFragment<MainVM>() {

    companion object {
        private const val TAG = "ChatFragment"
        private const val PAGE_SIZE = 20
        private const val MAX_VOICE_SECONDS = 60

        fun newInstance(peerId: String, peerName: String, peerAvatar: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString("peerId", peerId)
                    putString("peerName", peerName)
                    putString("peerAvatar", peerAvatar)
                }
            }
        }
    }

    override fun layoutId() = R.layout.fragment_chat

    override fun initVM() = MainVM()

    private val binding get() = mBinding as FragmentChatBinding

    private lateinit var peerId: String
    private var peerName = ""
    private var peerAvatar = ""

    private lateinit var adapter: ChatMessageAdapter
    private lateinit var lm: androidx.recyclerview.widget.LinearLayoutManager
    private lateinit var voiceRecorder: VoiceRecorder
    private var recording = false
    private var voiceStartAt = 0L

    //向上翻页状态位：防止滚动惯性/重复触发并发拉取；到顶无更多时不再触发
    private var loadingHistory = false
    private var hasMoreHistory = true

    override fun initShiTu() {
        peerId = arguments?.getString("peerId") ?: ""
        peerName = arguments?.getString("peerName") ?: ""
        peerAvatar = arguments?.getString("peerAvatar") ?: ""
        if (peerId.isBlank()) {
            ToastUtil.showMessage(getString(R.string.im_chat_invalid_peer))
            pop()
            return
        }

        binding.tvPeerName.text = peerName
        voiceRecorder = VoiceRecorder(requireContext())

        //官方账号会话不可回复：隐藏输入栏/录音条即可，底部不再显示提示文案
        val peerAccountId = com.fingertip.uilib.im.ImConfig.accountIdOf(peerId)
        val isOfficialPeer = com.fingertip.uilib.im.ImConfig.isOfficialAccount(peerAccountId)
        if (isOfficialPeer) {
            binding.llInputBar.visibility = View.GONE
            binding.llRecording.visibility = View.GONE
        } else {
            //对方可能从未登录过 IM（后台没注册），发消息会 6032 invalid receiver——
            //进页先让服务端把对方导入（幂等），失败静默
            if (peerAccountId > 0) importPeerQuietly(peerAccountId)
        }

        //软键盘顶起输入栏：容器 Activity 是全屏沉浸（isFullTopBar），adjustResize 失效，
        //自己监听 IME insets，把输入栏垫在键盘上方（取 IME 与导航栏的较大者）
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            val nav = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = maxOf(ime.bottom, nav.bottom))
            insets
        }

        adapter = ChatMessageAdapter(requireContext()).apply {
            peerAvatar = this@ChatFragment.peerAvatar
            onPreviewImage = { url ->
                GalleryPickerHelper.toPreViewImage(
                    requireActivity(), 0, arrayListOf(url)
                )
            }
            onOpenActivity = { activity ->
                try {
                    com.fingertip.uilib.activity.CommonWebViewActivity.start(
                        requireActivity(), activity.title, activity.url
                    )
                } catch (e: Exception) {
                    log(TAG, "打开活动页失败 ${e.message}")
                }
            }
        }
        lm = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvMsg.layoutManager = lm
        binding.rvMsg.adapter = adapter
        binding.rvMsg.itemAnimator = null
        //滚动到顶（用户主动上滑）触发加载更早的历史消息
        binding.rvMsg.addOnScrollListener(object :
            androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(
                rv: androidx.recyclerview.widget.RecyclerView,
                dx: Int,
                dy: Int
            ) {
                super.onScrolled(rv, dx, dy)
                //dy<0：用户主动上滑；第一条可见项已到顶 → 翻页
                if (dy < 0 && lm.findFirstVisibleItemPosition() <= 0) {
                    triggerLoadHistory()
                }
            }
        })
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack,
        binding.ivSend,
        binding.ivImage,
        binding.ivVideo,
        binding.ivVoice,
        binding.tvRecordingStop
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.ivSend -> sendText()
            binding.ivImage -> pickMedia(MediaType.IMAGE)
            binding.ivVideo -> pickMedia(MediaType.VIDEO)
            binding.ivVoice -> onVoiceClick()
            binding.tvRecordingStop -> stopVoiceAndSend()
        }
    }

    override fun initObserver() {
        super.initObserver()
    }

    override fun onVisible() {
        super.onVisible()
        if (adapter.data().isEmpty()) loadFirstPage()
        ImManager.activePeerId = peerId
        markRead()
    }

    override fun onInvisible() {
        super.onInvisible()
        if (ImManager.activePeerId == peerId) ImManager.activePeerId = null
    }

    // ==================== 历史消息 ====================

    /** 让服务端导入对方 IM 账号（幂等，静默失败） */
    private fun importPeerQuietly(targetAccountId: Int) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.fingertip.baselib.net.NetManager.getApi()
                    .importPeer(targetAccountId = targetAccountId)
            } catch (e: Exception) {
                log(TAG, "导入对方 IM 账号失败 ${e.message}")
            }
        }
    }
    private fun loadHistory(fromMsg: V2TIMMessage?) {
        if (!ImManager.loggedIn) {
            ToastUtil.showMessage(getString(R.string.im_chat_not_login))
            return
        }
        loadingHistory = true
        try {
            //云端拉取（含漫游）：换设备登录后同样能拉到完整历史
            val option = com.tencent.imsdk.v2.V2TIMMessageListGetOption().apply {
                getType = com.tencent.imsdk.v2.V2TIMMessageListGetOption.V2TIM_GET_CLOUD_OLDER_MSG
                userID = peerId
                count = PAGE_SIZE
                lastMsg = fromMsg
            }
            V2TIMManager.getMessageManager()
                .getHistoryMessageList(option,
                    object : V2TIMValueCallback<List<V2TIMMessage>> {
                        override fun onSuccess(t: List<V2TIMMessage>?) {
                            if (!isAdded) { loadingHistory = false; return }
                            val list = t.orEmpty()
                            //兼容 SDK 升/降序两种返回：统一按时间升序渲染
                            val ordered = if (list.size > 1 &&
                                list.first().timestamp > list.last().timestamp
                            ) list.reversed() else list

                            if (fromMsg == null) {
                                //首屏：云端结果覆盖本地（云端为准），定位到底部
                                if (ordered.isNotEmpty()) {
                                    adapter.initData(ordered)
                                    scrollBottom()
                                }
                                loadingHistory = false
                                return
                            }

                            //向上翻页分支
                            if (ordered.isEmpty()) {
                                //已无更早消息
                                hasMoreHistory = false
                                loadingHistory = false
                                ToastUtil.showMessage(getString(R.string.im_chat_history_no_more))
                                return
                            }
                            //插入头部并精确保持当前阅读位置，否则用户会被顶回顶部
                            val added = ordered.size
                            val firstPos = lm.findFirstVisibleItemPosition()
                            val topOffset = lm.findViewByPosition(firstPos)?.top ?: 0
                            val merged = ordered + adapter.data()
                            adapter.initData(merged)
                            lm.scrollToPositionWithOffset(firstPos + added, topOffset)
                            loadingHistory = false
                        }

                        override fun onError(code: Int, desc: String?) {
                            log(TAG, "历史消息拉取失败 code=$code desc=$desc")
                            loadingHistory = false
                        }
                    })
        } catch (e: Exception) {
            log(TAG, "历史消息异常 ${e.message}")
            loadingHistory = false
        }
    }

    /**
     * 到顶触发向上翻页（带 lastMsg 的 loadHistory）。
     * 守卫：正在加载 / 已无更多 / 列表空 时直接返回，避免滚动惯性重复触发。
     */
    private fun triggerLoadHistory() {
        if (loadingHistory || !hasMoreHistory) return
        if (adapter.data().isEmpty()) return
        val firstMsg = adapter.data().firstOrNull() ?: return
        loadHistory(firstMsg)
    }

    /**
     * 首屏：先读本地库（SDK 本地 DB，毫秒级，直接渲染消除空白页），
     * 再云端同步（补漫游/其他设备的新消息，云端结果为准）。
     * 云端先回而本地后回时，避免本地旧数据覆盖云端新数据：本地只在列表为空时渲染。
     */
    private fun loadFirstPage() {
        if (!ImManager.loggedIn) {
            ToastUtil.showMessage(getString(R.string.im_chat_not_login))
            return
        }
        val render: (List<V2TIMMessage>?) -> Unit = { list ->
            if (isAdded) {
                val items = list.orEmpty()
                if (items.isNotEmpty()) {
                    val ordered = if (items.size > 1 &&
                        items.first().timestamp > items.last().timestamp
                    ) items.reversed() else items
                    adapter.initData(ordered)
                    scrollBottom()
                }
            }
        }
        try {
            val localOption = com.tencent.imsdk.v2.V2TIMMessageListGetOption().apply {
                getType = com.tencent.imsdk.v2.V2TIMMessageListGetOption.V2TIM_GET_LOCAL_OLDER_MSG
                userID = peerId
                count = PAGE_SIZE
            }
            V2TIMManager.getMessageManager().getHistoryMessageList(localOption,
                object : V2TIMValueCallback<List<V2TIMMessage>> {
                    override fun onSuccess(t: List<V2TIMMessage>?) {
                        //云端结果可能已经先到了：只在当前无数据时用本地兜底渲染
                        if (adapter.data().isEmpty()) render(t)
                    }

                    override fun onError(code: Int, desc: String?) {}
                })
        } catch (e: Exception) {
            log(TAG, "本地消息读取异常 ${e.message}")
        }
        loadHistory(null)
    }

    private fun scrollBottom() {
        // 注意：不能用 post 延迟。post 会让 RecyclerView 先完成首帧布局（停在顶部 position 0），
        // 等下一帧再 scrollToPosition，视觉上变成「先显示顶部、再滑动到底部」。
        // 同步调用会把 scroll 目标写进 mPendingScrollPosition，随 initData 的布局同帧处理，首帧即定位底部。
        val count = adapter.itemCount
        if (count > 0) binding.rvMsg.scrollToPosition(count - 1)
    }

    private fun markRead() {
        try {
            V2TIMManager.getMessageManager().markC2CMessageAsRead(peerId, null)
        } catch (_: Exception) {
        }
    }

    // ==================== 发送 ====================

    private fun sendText() {
        val text = binding.etInput.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        binding.etInput.setText("")
        send { V2TIMManager.getMessageManager().createTextMessage(text) }
    }

    private fun send(creator: () -> V2TIMMessage?) {
        if (!ImManager.loggedIn) {
            ToastUtil.showMessage(getString(R.string.im_chat_not_login))
            return
        }
        try {
            val msg = creator()
            if (msg == null) {
                ToastUtil.showMessage(getString(R.string.im_chat_send_fail))
                return
            }
            V2TIMManager.getMessageManager().sendMessage(
                msg, peerId, null, V2TIMMessage.V2TIM_PRIORITY_DEFAULT, false, null,
                object : com.tencent.imsdk.v2.V2TIMSendCallback<V2TIMMessage> {
                    override fun onProgress(progress: Int) {}

                    override fun onSuccess(t: V2TIMMessage?) {
                        if (!isAdded) return
                        t?.let {
                            adapter.addData(it)
                            scrollBottom()
                        }
                    }

                    override fun onError(code: Int, desc: String?) {
                        log(TAG, "发送失败 code=$code desc=$desc")
                        ToastUtil.showMessage(getString(R.string.im_chat_send_fail))
                    }
                })
        } catch (e: Exception) {
            log(TAG, "发送异常 ${e.message}")
            ToastUtil.showMessage(getString(R.string.im_chat_send_fail))
        }
    }

    private fun pickMedia(type: MediaType) {
        try {
            GalleryPickerHelper.newInstance()
                .maxItems(1)
                .maxVideoSize(100)
                .launchMediaPicker(requireActivity(), type, object : MediaResultCallback {
                    override fun onMediaResult(mediaFiles: List<MediaData>) {
                        val file = mediaFiles.firstOrNull() ?: return
                        val path = file.filePath
                        if (path.isBlank()) return
                        when (type) {
                            MediaType.IMAGE -> uploadAndSendImage(path)
                            MediaType.VIDEO -> uploadAndSendVideo(path)
                            else -> {}
                        }
                    }
                })
        } catch (e: Exception) {
            log(TAG, "媒体选择异常 ${e.message}")
        }
    }

    // ==================== 媒体发送（文件传自有 OSS，IM 只发 JSON） ====================

    private fun imObjectKey(path: String): String =
        "app/im/${System.currentTimeMillis()}_${(1000..9999).random()}_${java.io.File(path).name}"

    private fun sendCustomJson(json: String) {
        //本版 SDK createCustomMessage 只收 byte[]，data 即 JSON 字符串
        send { V2TIMManager.getMessageManager().createCustomMessage(json.toByteArray()) }
    }

    private fun uploadAndSendImage(path: String) {
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, opts)
        ToastUtil.showMessage(getString(R.string.im_chat_uploading))
        PicUtils.uploadToOss(
            GlobalConfig.globalParam?.bucketName ?: "", imObjectKey(path), path,
            object : PicUtils.OssUploadCallback {
                override fun onProgress(currentSize: Long, totalSize: Long) {}
                override fun onSuccess(objectUrl: String) {
                    val json = ImMsgUtil.buildMediaJson(
                        MediaMsg(mediaType = "image", url = objectUrl,
                            width = opts.outWidth, height = opts.outHeight)
                    )
                    binding.root.post { sendCustomJson(json) }
                }

                override fun onFailure(errorMsg: String) {
                    log(TAG, "图片上传失败 $errorMsg")
                    ToastUtil.showMessage(getString(R.string.im_chat_upload_fail))
                }
            })
    }

    private fun uploadAndSendVideo(path: String) {
        ToastUtil.showMessage(getString(R.string.im_chat_uploading))
        //时长与封面帧
        var durationSec = 0
        var thumbFile: java.io.File? = null
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(path)
            durationSec = ((retriever.extractMetadata(
                android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L) / 1000).toInt()
            retriever.getFrameAtTime(0)?.let { bmp ->
                val dir = java.io.File(requireContext().cacheDir, "im_video").apply { mkdirs() }
                thumbFile = java.io.File(dir, "thumb_${System.currentTimeMillis()}.jpg").also {
                    it.outputStream().use { os -> bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, os) }
                }
                bmp.recycle()
            }
            retriever.release()
        } catch (e: Exception) {
            log(TAG, "视频封面提取失败 ${e.message}")
        }
        val thumb = thumbFile
        val bucket = GlobalConfig.globalParam?.bucketName ?: ""
        fun onDone(videoUrl: String, thumbUrl: String) {
            val json = ImMsgUtil.buildMediaJson(
                MediaMsg(mediaType = "video", url = videoUrl, duration = durationSec, thumb = thumbUrl)
            )
            binding.root.post { sendCustomJson(json) }
        }
        if (thumb == null) {
            //封面提取失败也照发，接收端用默认占位图
            PicUtils.uploadToOss(bucket, imObjectKey(path), path,
                object : PicUtils.OssUploadCallback {
                    override fun onProgress(currentSize: Long, totalSize: Long) {}
                    override fun onSuccess(objectUrl: String) = onDone(objectUrl, "")
                    override fun onFailure(errorMsg: String) {
                        log(TAG, "视频上传失败 $errorMsg")
                        ToastUtil.showMessage(getString(R.string.im_chat_upload_fail))
                    }
                })
            return
        }
        //视频 + 封面两个文件一起传
        PicUtils.batchUploadToOss(
            bucket,
            listOf(imObjectKey(path), imObjectKey(thumb.absolutePath)),
            listOf(path, thumb.absolutePath),
            object : PicUtils.OssBatchUploadCallback {
                override fun onBatchStart(totalCount: Int) {}
                override fun onFileProgress(index: Int, objectKey: String, currentSize: Long, totalSize: Long) {}
                override fun onFileSuccess(index: Int, objectKey: String, objectUrl: String) {}
                override fun onFileFailed(index: Int, objectKey: String, errorMsg: String) {
                    log(TAG, "视频上传失败 $errorMsg")
                    ToastUtil.showMessage(getString(R.string.im_chat_upload_fail))
                }
                override fun onOverallProgress(completedCount: Int, totalCount: Int) {}
                override fun onBatchComplete(results: List<PicUtils.BatchFileResult>) {
                    val video = results.firstOrNull { !it.objectUrl.isNullOrBlank() && it.localFilePath == path }
                    if (video == null) {
                        ToastUtil.showMessage(getString(R.string.im_chat_upload_fail))
                        return
                    }
                    val cover = results.firstOrNull { !it.objectUrl.isNullOrBlank() && it.localFilePath == thumb.absolutePath }
                    onDone(video.objectUrl ?: "", cover?.objectUrl ?: "")
                }
                override fun onAllFailed(errorMsg: String) {
                    log(TAG, "视频上传失败 $errorMsg")
                    ToastUtil.showMessage(getString(R.string.im_chat_upload_fail))
                }
            })
    }

    private fun uploadAndSendSound(path: String, seconds: Int) {
        ToastUtil.showMessage(getString(R.string.im_chat_uploading))
        PicUtils.uploadToOss(
            GlobalConfig.globalParam?.bucketName ?: "", imObjectKey(path), path,
            object : PicUtils.OssUploadCallback {
                override fun onProgress(currentSize: Long, totalSize: Long) {}
                override fun onSuccess(objectUrl: String) {
                    val json = ImMsgUtil.buildMediaJson(
                        MediaMsg(mediaType = "sound", url = objectUrl, duration = seconds)
                    )
                    binding.root.post { sendCustomJson(json) }
                }

                override fun onFailure(errorMsg: String) {
                    log(TAG, "语音上传失败 $errorMsg")
                    ToastUtil.showMessage(getString(R.string.im_chat_upload_fail))
                }
            })
    }

    // ==================== 语音 ====================

    private fun onVoiceClick() {
        if (recording) {
            stopVoiceAndSend()
            return
        }
        if (checkVoicePermission()) startVoice()
    }

    private fun checkVoicePermission(): Boolean = try {
        requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }.also { granted ->
        if (!granted) voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private val voicePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startVoice() else ToastUtil.showMessage(getString(R.string.im_chat_voice_need_perm))
        }

    private fun startVoice() {
        if (voiceRecorder.start() == null) {
            ToastUtil.showMessage(getString(R.string.im_chat_voice_start_fail))
            return
        }
        recording = true
        voiceStartAt = System.currentTimeMillis()
        binding.llInputBar.visibility = View.GONE
        binding.llRecording.visibility = View.VISIBLE
    }

    private fun stopVoiceAndSend() {
        val result = voiceRecorder.stop()
        recording = false
        binding.llInputBar.visibility = View.VISIBLE
        binding.llRecording.visibility = View.GONE
        if (result == null) {
            ToastUtil.showMessage(getString(R.string.im_chat_voice_start_fail))
            return
        }
        val (path, seconds) = result
        if (System.currentTimeMillis() - voiceStartAt < 800) {
            //误触：不足 1 秒视为取消
            java.io.File(path).delete()
            return
        }
        if (seconds > MAX_VOICE_SECONDS) {
            ToastUtil.showMessage(getString(R.string.im_chat_voice_too_long))
            java.io.File(path).delete()
            return
        }
        uploadAndSendSound(path, seconds)
    }

    override fun onDestroyView() {
        if (recording) voiceRecorder.cancel()
        ImManager.activePeerId = null
        super.onDestroyView()
    }

    // ==================== 新消息到达 ====================

    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        if (!isAdded) return
        if (event.what != EventConstant.EVENT_IM_NEW_MESSAGE) return
        val msg = event.`object` as? V2TIMMessage ?: return
        if (msg.sender != peerId) return
        //正在看的会话：消息追加到底部（通知被 activePeerId 抑制）
        adapter.addData(msg)
        scrollBottom()
    }
}

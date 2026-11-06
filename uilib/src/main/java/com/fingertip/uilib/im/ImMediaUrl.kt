package com.fingertip.uilib.im

import com.fingertip.baselib.log
import com.fingertip.baselib.net.NetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 私有 OSS 文件 URL → 带签名临时 URL
 *
 * IM 媒体消息里存的是裸 OSS URL（b-fingertip 桶是私有的，直接访问 403），
 * 渲染/播放前先经服务端 /config/getAccessUrl 换签名 URL。
 * 内存缓存 + 过期提前量；非自有 OSS 的 URL（腾讯 COS、活动封面等）原样透传。
 */
object ImMediaUrl {

    private const val TAG = "ImMediaUrl"
    private const val HOST_MARK = "aliyuncs.com"
    private const val CACHE_MS = 50 * 60 * 1000L   //服务端默认 1h 签名，保守 50 分钟

    private val cache = mutableMapOf<String, Pair<Long, String>>()

    /** 拿可访问 URL：命中缓存同步回调，否则异步签名后回调（主线程） */
    fun resolve(raw: String, onReady: (String) -> Unit) {
        if (raw.isBlank() || !raw.contains(HOST_MARK)) {
            onReady(raw)
            return
        }
        val now = System.currentTimeMillis()
        val cached = synchronized(cache) { cache[raw] }
        if (cached != null && cached.first > now + 60_000) {
            onReady(cached.second)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val signed = try {
                val rsp = NetManager.getApi().getAccessUrl(fileUrl = raw)
                val map = rsp.data
                map?.get(raw).takeIf { !it.isNullOrBlank() } ?: raw
            } catch (e: Exception) {
                log(TAG, "签名失败 ${e.message}")
                raw
            }
            if (signed != raw) synchronized(cache) { cache[raw] = (now + CACHE_MS) to signed }
            withContext(Dispatchers.Main) { onReady(signed) }
        }
    }
}

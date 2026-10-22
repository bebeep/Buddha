package com.fingertip.baselib.download

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.BookChapter
import com.fingertip.baselib.bean.BookChapterItem
import com.fingertip.baselib.db.ChapterStore
import com.fingertip.baselib.net.NetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * 整部经离线下载
 *
 * 挂在单例上而不是页面里：退出阅读页下载照样往前走，
 * 进度用 LiveData 广播，当前可见的页面自己决定怎么显示。
 * 已经缓存的章不会再下，断了从缺的地方接着来
 */
object ChapterDownloader {

    //一批拉多少章，与服务端单批上限对齐
    private const val BATCH_SIZE = 20

    //单批失败重试次数
    private const val RETRY = 2

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val state = MutableLiveData<DownloadState>()

    val progress: LiveData<DownloadState> = state

    private var job: Job? = null

    /**
     * 只刷新「已经缓存了多少章」，用于进页面时把状态摆正
     */
    fun refresh(bookId: Int, total: Int) {
        if (job?.isActive == true && state.value?.bookId == bookId) return
        scope.launch {
            val done = ChapterStore.getCachedSeqs(bookId).size
            state.postValue(DownloadState(bookId, done, total, false))
        }
    }

    fun isDownloading(bookId: Int): Boolean {
        return job?.isActive == true && state.value?.bookId == bookId
    }

    /**
     * 开始下载。只下本地没有的章，中途退出或失败都不会丢已下好的部分
     */
    fun start(bookId: Int, catalog: List<BookChapterItem>) {
        if (job?.isActive == true) return
        val total = catalog.size
        if (bookId <= 0 || total <= 0) return
        job = scope.launch {
            val cached = ChapterStore.getCachedHashes(bookId)
            var done = cached.size
            state.postValue(DownloadState(bookId, done, total, true))

            //缺的章要下；本地有但指纹跟目录不一致的，是服务端改过正文，也要重下
            val catalogHash = catalog.associate { it.seq to it.contentHash }
            val missing = catalog.map { it.seq }.filter { seq ->
                val local = cached[seq]
                val remote = catalogHash[seq]
                //指纹为空说明服务端没给，不敢当成过期，免得白下一遍
                local == null ||
                    (local.isNotEmpty() && !remote.isNullOrEmpty() && local != remote)
            }
            var index = 0
            while (index < missing.size && isActive) {
                val startSeq = missing[index]
                //连续缺的凑一批，中间有洞就断开，别把已经有的章又拉一遍
                var endIndex = index
                while (endIndex + 1 < missing.size
                    && missing[endIndex + 1] == missing[endIndex] + 1
                    && endIndex + 1 - index < BATCH_SIZE
                ) {
                    endIndex++
                }
                val list = fetchBatch(bookId, startSeq, missing[endIndex])
                if (list == null) {
                    state.postValue(
                        DownloadState(bookId, done, total, false, "下载中断了，已下好的章节保留")
                    )
                    return@launch
                }
                list.forEach { ChapterStore.saveChapter(it) }
                done = ChapterStore.getCachedSeqs(bookId).size
                state.postValue(DownloadState(bookId, done, total, true))
                index = endIndex + 1
            }

            //协程被取消时不再改状态，交给 cancel 收拾
            if (!isActive) return@launch
            state.postValue(DownloadState(bookId, done, total, false))
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        val current = state.value ?: return
        state.postValue(current.copy(running = false, error = null))
    }

    private suspend fun fetchBatch(bookId: Int, startSeq: Int, endSeq: Int): List<BookChapter>? {
        repeat(RETRY) {
            val rsp = try {
                NetManager.getApi().getBookChapters(
                    bookId = bookId, startSeq = startSeq, endSeq = endSeq
                )
            } catch (e: Exception) {
                null
            }
            if (rsp != null && rsp.code == 0 && !rsp.data.isNullOrEmpty()) {
                return rsp.data
            }
        }
        return null
    }
}


/**
 * 这本书的下载进度，一次只能下一部，所以带个 bookId 区分是谁的
 */
data class DownloadState(
    val bookId: Int,
    val done: Int,
    val total: Int,
    val running: Boolean,
    val error: String? = null
) {
    fun percent(): Int = if (total <= 0) 0 else done * 100 / total

    fun finished(): Boolean = total > 0 && done >= total
}

package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.BookChapter
import com.fingertip.baselib.bean.BookChapterItem
import com.fingertip.baselib.bean.BookCopySetting
import com.fingertip.baselib.bean.BookProgress
import com.fingertip.baselib.db.ChapterStore
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp


/**
 * 抄经。目录和正文跟读经走同一套缓存，进度单独记：
 * 抄经是逐字往前推的游标，本地只存「抄到哪一章第几个字 + 一共抄了多少字」
 */
class CopyVM : TopVMImp() {

    val catalogResult = MutableLiveData<RequestResult<List<BookChapterItem>>>()
    val chapterResult = MutableLiveData<RequestResult<BookChapter>>()
    val progressResult = MutableLiveData<RequestResult<BookProgress>>()
    val reportResult = MutableLiveData<RequestResult<BookProgress>>()
    val settingResult = MutableLiveData<RequestResult<BookCopySetting>>()
    val saveSettingResult = MutableLiveData<RequestResult<BookCopySetting>>()

    //网络目录已经回来的标记，避免本地缓存晚一步把新数据顶掉
    private var catalogFromNetwork = false

    /**
     * 目录：本地有就先渲染，同时用网络刷新一遍
     */
    fun loadCatalog(bookId: Int) {
        catalogFromNetwork = false
        launchUI {
            val local = ChapterStore.getCatalog(bookId)
            if (local.isNotEmpty() && !catalogFromNetwork) {
                catalogResult.value = successResult(local)
            }
        }
        call({
            NetManager.getApi().getBookChapterList(bookId = bookId)
        }, {
            catalogFromNetwork = true
            val remote = it ?: emptyList()
            if (remote.isNotEmpty()) {
                catalogResult.value = successResult(remote)
                launchUI { ChapterStore.saveCatalog(bookId, remote) }
            }
        }, {
            catalogResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    /**
     * 正文：本地有就直接用，并把本地指纹带给服务端比对，一致则不再下发正文
     */
    fun loadChapter(bookId: Int, seq: Int) {
        launchUI {
            val local = ChapterStore.getChapter(bookId, seq)
            if (local != null) {
                chapterResult.value = successResult(local)
            }
            requestChapter(bookId, seq, local?.contentHash ?: "")
        }
    }

    private fun requestChapter(bookId: Int, seq: Int, contentHash: String) {
        call({
            NetManager.getApi().getBookChapter(bookId = bookId, seq = seq, contentHash = contentHash)
        }, {
            val chapter = it
            if (chapter == null) {
                chapterResult.value = failResult()
                return@call
            }
            if (chapter.notModified) {
                //本地已是最新，不重复渲染，也不落库
                return@call
            }
            chapterResult.value = successResult(chapter)
            launchUI { ChapterStore.saveChapter(chapter) }
        }, {
            chapterResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    /**
     * 抄经进度，进入抄经页时决定从哪一章哪个字开始
     */
    fun loadProgress(bookId: Int) {
        call({
            NetManager.getApi().getBookProgress(bookId = bookId)
        }, {
            progressResult.value = successResult(it)
        }, {
            progressResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    /**
     * 断网兜底：位置和累计字数先落 Room
     */
    fun saveLocalProgress(bookId: Int, seq: Int, offset: Int, copiedChars: Long) {
        launchUI { ChapterStore.saveCopyProgress(bookId, seq, offset, copiedChars) }
    }

    /**
     * 上报抄经进度。chars、skippedChars 都传本次增量，服务端做累加
     */
    fun reportCopyProgress(bookId: Int, seq: Int, offset: Int, chars: Int,
                           durationMs: Int, skippedChars: Int) {
        call({
            NetManager.getApi().updateCopyProgress(
                request = RequestBodyFactory.buildCopyProgressBody(
                    bookId = bookId,
                    seq = seq,
                    charOffset = offset,
                    chars = chars,
                    durationMs = durationMs,
                    skippedChars = skippedChars
                )
            )
        }, {
            reportResult.value = successResult(it)
        }, {
            reportResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    /**
     * 抄经显示设置
     */
    fun loadCopySetting() {
        call({
            NetManager.getApi().getCopySetting()
        }, {
            settingResult.value = successResult(it)
        }, {
            settingResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    /**
     * 保存抄经显示设置，服务端存一份，换手机也跟着走
     */
    fun saveCopySetting(setting: BookCopySetting) {
        call({
            NetManager.getApi().updateCopySetting(
                request = RequestBodyFactory.buildCopySettingBody(
                    defaultColor = setting.defaultColor,
                    copyColor = setting.copyColor,
                    backgroundColor = setting.backgroundColor,
                    fontSizeLevel = setting.fontSizeLevel
                )
            )
        }, {
            saveSettingResult.value = successResult(it)
            if (it != null) settingResult.value = successResult(it)
        }, {
            saveSettingResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }
}

package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.BookChapter
import com.fingertip.baselib.bean.BookChapterItem
import com.fingertip.baselib.bean.BookProgress
import com.fingertip.baselib.bean.BookReadSetting
import com.fingertip.baselib.db.ChapterStore
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp


/**
 * 读经。目录与正文都先给本地缓存，再用网络结果刷新，
 * 断网时打开已缓存过的经书照样能读
 */
class ReadVM : TopVMImp() {

    val catalogResult = MutableLiveData<RequestResult<List<BookChapterItem>>>()
    val chapterResult = MutableLiveData<RequestResult<BookChapter>>()
    val progressResult = MutableLiveData<RequestResult<BookProgress>>()
    val settingResult = MutableLiveData<RequestResult<BookReadSetting>>()
    val saveSettingResult = MutableLiveData<RequestResult<BookReadSetting>>()

    //网络目录已经回来的标记，避免本地缓存晚一步把新数据顶掉
    private var catalogFromNetwork = false

    //本地进度落库的节流起点
    private var lastStoreAt = 0L

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
     * 读经进度，用于进入阅读页时恢复位置
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
     * 上报读经进度
     */
    fun updateReadProgress(bookId: Int, seq: Int, charOffset: Int) {
        call({
            NetManager.getApi().updateReadProgress(
                request = RequestBodyFactory.buildReadProgressBody(bookId, seq, charOffset)
            )
        }, {
            progressResult.value = successResult(it)
        }, {
            progressResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    /**
     * 阅读位置：滚动停下先落 Room（1 秒节流），服务端等退出阅读页再上报
     */
    fun storeReadProgress(bookId: Int, seq: Int, offset: Int) {
        val now = System.currentTimeMillis()
        if (now - lastStoreAt < 1000) return
        lastStoreAt = now
        launchUI { ChapterStore.saveReadProgress(bookId, seq, offset) }
    }

    /**
     * 退出阅读页：位置立即落 Room，并同步给服务端
     */
    fun reportReadProgress(bookId: Int, seq: Int, offset: Int) {
        lastStoreAt = System.currentTimeMillis()
        launchUI { ChapterStore.saveReadProgress(bookId, seq, offset) }
        updateReadProgress(bookId, seq, offset)
    }

    /**
     * 读经显示设置
     */
    fun loadReadSetting() {
        call({
            NetManager.getApi().getReadSetting()
        }, {
            settingResult.value = successResult(it)
        }, {
            settingResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    /**
     * 保存读经显示设置，服务端存一份，换手机也跟着走
     */
    fun saveReadSetting(setting: BookReadSetting) {
        call({
            NetManager.getApi().updateReadSetting(
                request = RequestBodyFactory.buildReadSettingBody(
                    textColor = setting.textColor,
                    backgroundColor = setting.backgroundColor,
                    fontSizeLevel = setting.fontSizeLevel,
                    lineSpacingLevel = setting.lineSpacingLevel,
                    keepScreenOn = setting.keepScreenOn,
                    showTrad = setting.showTrad
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

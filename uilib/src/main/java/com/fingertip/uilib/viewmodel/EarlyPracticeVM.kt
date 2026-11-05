package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.PracticeStatisticData
import com.fingertip.baselib.db.ChapterStore
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone
import java.util.Calendar

/**
 * 早课诵经卡上展示的一条经文
 */
data class DailySutra(
    //经书名，如《金刚经》
    val bookTitle: String,
    //章节名，有品名用品名，没有用卷题
    val chapterTitle: String,
    //简体正文
    val content: String
)

/**
 * 早课五步流程。对应服务端 practice 一组接口。
 *
 * 关键点：时区必须由客户端上报——服务端要知道用户本地现在是不是早晨，
 * 才能判定这次提交算早课还是晚课，也才能算对"今天是哪天"。
 * 用 [TimeZone.getDefault().id]，得到 Asia/Singapore、Asia/Kuala_Lumpur 这种 IANA id。
 *
 * 步骤进度在页面本地维护，全部走完后才调 [submit]；
 * 中途退出不留服务端记录，用户第二天可以重新开始（一天只记一次是服务端硬约束）。
 */
class EarlyPracticeVM : TopVMImp() {

    //提交结果
    val submitResult = MutableLiveData<RequestResult<String>>()

    //连续统计（进页时拉一次，用来判断今天是否已完成 + 结果页展示连续天数）
    val statisticResult = MutableLiveData<RequestResult<PracticeStatisticData>>()

    //早课诵经卡上的随机经文，拉不到就保持布局里写死的心经兜底
    val sutraResult = MutableLiveData<DailySutra>()

    /**
     * 每日随机取一段经文给早课第二步诵经。
     *
     * 随机不是纯随机的：种子 = 当天日期 + 用户 session，
     * 所以同一天内反复进出早课看到的是同一段（退出重进内容不变），
     * 第二天自动换，不同用户也不同。书架优先，书架为空时从全部专题里随机挑。
     * 任何一步失败都静默放弃，交给页面兜底文案，不弹错误打扰修持流程。
     */
    fun loadDailySutra() {
        launchUI {
            try {
                //书架优先，空了再全库随机
                var books = NetManager.getApi().getBookInShelf(currPage = 1).data.orEmpty()
                if (books.isEmpty()) {
                    val themes = NetManager.getApi().getBookThemeList().data.orEmpty()
                    val theme = themes[Random(seed().hashCode().toLong()).nextInt(themes.size)]
                    books = NetManager.getApi()
                        .getBookByTheme(themeId = theme.id, currPage = 1).data.orEmpty()
                }
                if (books.isEmpty()) return@launchUI

                val rng = Random(seed().hashCode().toLong())
                val book = books[rng.nextInt(books.size)]

                //目录：本地缓存优先，网络兜底并落库
                var catalog = ChapterStore.getCatalog(book.bookId)
                if (catalog.isEmpty()) {
                    catalog = NetManager.getApi().getBookChapterList(bookId = book.bookId).data.orEmpty()
                    if (catalog.isEmpty()) return@launchUI
                    ChapterStore.saveCatalog(book.bookId, catalog)
                }
                //跳过空章
                val readable = catalog.filter { it.charCount > 0 }.ifEmpty { catalog }
                val item = readable[rng.nextInt(readable.size)]

                //正文：本地有就用，没有再拉网络并缓存
                val chapter = ChapterStore.getChapter(book.bookId, item.seq)
                    ?: NetManager.getApi()
                        .getBookChapter(bookId = book.bookId, seq = item.seq, contentHash = "").data
                    ?.also { ChapterStore.saveChapter(it) }
                val content = chapter?.content.orEmpty()
                if (content.isBlank()) return@launchUI

                sutraResult.value = DailySutra(book.bookTitle, item.displayTitle(), content)
            } catch (_: Exception) {
                //静默：早课流程不被一次取经失败打断
            }
        }
    }

    //当天日期 + 用户标识做随机种子，同一天重进内容稳定
    private fun seed(): String {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return "$day|${GlobalConfig.session}"
    }

    /**
     * 提交早课
     * @param stepData 五步明细 json
     * @param totalSeconds 从进入页面到完成的总秒数，服务端做时长下限校验
     */
    fun submit(stepData: String, totalSeconds: Int) {
        call({
            NetManager.getApi().submitPractice(
                body = RequestBodyFactory.practiceSubmitBody(
                    stepData = stepData,
                    totalSeconds = totalSeconds,
                    timeZone = TimeZone.getDefault().id
                )
            )
        }, {
            submitResult.value = successResult(it)
        }, {
            submitResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 拉连续统计。未登录时服务端回 INVALID_SESSION，页面静默忽略。
     */
    fun loadStatistic() {
        call({
            NetManager.getApi().getPracticeStatistic()
        }, {
            statisticResult.value = successResult(it)
        }, {
            statisticResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

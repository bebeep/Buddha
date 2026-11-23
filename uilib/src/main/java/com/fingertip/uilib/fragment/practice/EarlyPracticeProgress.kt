package com.fingertip.uilib.fragment.practice

import android.content.Context
import com.fingertip.baselib.constant.GlobalConfig
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 早课分步完成态的共享读取入口。
 *
 * 首页「今日修持」和早课页共用同一份数据：
 * - 前四步（礼佛/诵经/念佛/发愿）读本地按天保存的 progress；
 * - 第五步「回向」以服务端 practiceDoneToday 为准（提交成功即完成）；
 * - 服务端确认今日已完成时，五步一律视为完成，并回填一份到本地，
 *   换设备登录后不用等下一次网络请求也能拿到正确状态。
 *
 * 本地状态按账号隔离：同一台机器换号登录，不会看到上一个账号的完成态。
 *
 * 常量与 [EarlyPracticeFragment] 共用，避免两处阈值漂移。
 */
object EarlyPracticeProgress {

    const val SP_NAME = "early_practice"
    const val SP_KEY_PROGRESS = "progress"
    const val SP_KEY_PROGRESS_DATE = "progress_date"
    const val SP_KEY_VOW = "last_vow"
    const val SP_KEY_VOW_HISTORY = "vow_history"
    //进度归属账号（换号不串状态）
    const val SP_KEY_ACCOUNT_ID = "account_id"
    //服务端已确认今日完成的日期，跨设备登录时用它判定是否需要回填
    const val SP_KEY_SERVER_DONE_DATE = "server_done_date"

    const val WORSHIP_TARGET = 3
    const val NIANFO_TARGET = 108

    private fun todayStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun currentAccountId(): Int = GlobalConfig.userData?.accountId ?: 0

    private fun JSONObject.isServerDone(): Boolean = optBoolean("serverDone", false)

    /**
     * 服务端确认今日早课已完成 → 回填五步本地完成态。
     *
     * 换设备登录后本地 SharedPreferences 是空的，首页聚合接口或早课统计接口
     * 只要带回完成态就落一份，首页五格与早课页恢复都按这份状态走。
     * 愿文只存本机、不上传，回填时保留本机已有的原文，没有就留空，不编造。
     */
    fun syncFromServer(context: Context, practiceDoneToday: Boolean) {
        if (!practiceDoneToday) return
        val accountId = currentAccountId()
        if (accountId <= 0) return //游客态不落进度，避免串到下一个登录用户

        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val today = todayStr()
        val savedAccount = sp.getString(SP_KEY_ACCOUNT_ID, "").orEmpty()
        val raw = sp.getString(SP_KEY_PROGRESS, "").orEmpty()
        val alreadySynced = sp.getString(SP_KEY_PROGRESS_DATE, "") == today
                && sp.getString(SP_KEY_SERVER_DONE_DATE, "") == today
                && savedAccount == accountId.toString()
                && raw.isNotBlank()
                && runCatching { JSONObject(raw).isServerDone() }.getOrDefault(false)
        if (alreadySynced) return

        //同账号同日已有的愿文原样保留；换设备/换号时本来就没有，不造一条假的
        val vow = if (sp.getString(SP_KEY_PROGRESS_DATE, "") == today
            && (savedAccount.isBlank() || savedAccount == accountId.toString())
            && raw.isNotBlank()
        ) {
            runCatching { JSONObject(raw).optString("vow", "") }.getOrDefault("")
        } else ""

        val progress = JSONObject().apply {
            put("worship", WORSHIP_TARGET)
            put("chant", true)
            put("nianfo", NIANFO_TARGET)
            put("vow", vow)
            put("serverDone", true)
        }
        sp.edit()
            .putString(SP_KEY_PROGRESS, progress.toString())
            .putString(SP_KEY_PROGRESS_DATE, today)
            .putString(SP_KEY_SERVER_DONE_DATE, today)
            .putString(SP_KEY_ACCOUNT_ID, accountId.toString())
            .apply()
    }

    /** 本地进度是否属于当前账号（老数据没有账号字段时按当前账号兼容） */
    fun belongsToCurrentAccount(savedAccount: String?): Boolean {
        if (savedAccount.isNullOrBlank()) return true
        return savedAccount == currentAccountId().toString()
    }

    /** 五步完成态：0礼佛 1诵经 2念佛 3发愿 4回向 */
    fun stepDoneFlags(context: Context, practiceDoneToday: Boolean): BooleanArray {
        //服务端已判定今日早课完成：五步全亮，跨设备/清本地数据也一致
        if (practiceDoneToday) return BooleanArray(5) { true }

        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val today = todayStr()
        if (!belongsToCurrentAccount(sp.getString(SP_KEY_ACCOUNT_ID, ""))) return BooleanArray(5)
        if (sp.getString(SP_KEY_PROGRESS_DATE, "") != today) return BooleanArray(5)

        val raw = sp.getString(SP_KEY_PROGRESS, "").orEmpty()
        if (raw.isBlank()) return BooleanArray(5)

        return try {
            val p = JSONObject(raw)
            //服务端已回填的完成态直接全亮，不再依赖本机愿文等非权威字段
            if (p.isServerDone()) return BooleanArray(5) { true }
            booleanArrayOf(
                p.optInt("worship", 0) >= WORSHIP_TARGET,
                p.optBoolean("chant", false),
                p.optInt("nianfo", 0) >= NIANFO_TARGET,
                p.optString("vow", "").trim().isNotBlank(),
                false //回向本地不存，只有服务端提交成功才算完成
            )
        } catch (_: Exception) {
            BooleanArray(5)
        }
    }
}

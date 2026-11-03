package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.HomeSummaryData
import com.fingertip.baselib.bean.TodayBuddhaInfo
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 首页。一次聚合请求拿回六区块要用的数据，
 * 佛历文案仍走单独的 account/getTodayBuddhaInfo（首页头卡要节日名，聚合接口里没有）。
 *
 * 失败都不弹 toast：首页是启动后的第一屏，弱网时弹一堆提示比空着更糟，
 * 页面自己决定显示兜底文案还是重试条。
 */
class HomeVM : TopVMImp() {

    //首页聚合数据（修持进度 / 供灯状态 / 禅值余额 / 连签天数 / 区块配置）
    val summaryResult = MutableLiveData<RequestResult<HomeSummaryData>>()

    //首页头卡的佛历日期与今日佛事
    val todayBuddhaResult = MutableLiveData<RequestResult<TodayBuddhaInfo>>()

    //请求进行中，防止下拉刷新连点造成重复请求
    private var summaryLoading = false

    /**
     * 首页聚合数据。timeZone 传本机时区，服务端据此判断「今天」是哪天
     * （海外用户跨时区时，早课/晚课归属才不会错）
     */
    fun loadHomeSummary() {
        if (summaryLoading) return
        summaryLoading = true
        call({
            NetManager.getApi().getHomeSummary()
        }, {
            summaryLoading = false
            summaryResult.value = successResult(it)
        }, {
            summaryLoading = false
            summaryResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 今日佛历信息，头卡的节日/斋日文案用
     */
    fun loadTodayBuddhaInfo() {
        call({
            NetManager.getApi().getTodayBuddhaInfo()
        }, {
            todayBuddhaResult.value = successResult(it)
        }, {
            todayBuddhaResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

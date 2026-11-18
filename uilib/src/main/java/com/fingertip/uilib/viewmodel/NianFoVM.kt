package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.NianfoToday
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 念佛静心页 VM。
 *
 * 修行值结算在服务端：客户端每念满一藏(108声)调一次 nianfoRoundReport，
 * 服务端按 global_param.expInNianfo 加修行值（不超过 expPerdayInNianfo 当日上限），
 * 落 user_nianfo_record 并返回本次实际增加的修行值（0 = 当日已达上限）。
 * 客户端不做修行值计算——否则改个本地变量就能刷。
 */
class NianFoVM : TopVMImp() {

    //一藏上报结果：data = 本次实际增加的修行值
    val roundReportResult = MutableLiveData<RequestResult<Int>>()

    //当日进度：已获得修行值 + 已完成轮数（进页拉取）
    val todayExpResult = MutableLiveData<RequestResult<NianfoToday>>()

    fun reportRound() {
        call({
            NetManager.getApi().reportNianfoRound()
        }, {
            roundReportResult.value = successResult(it)
        }, {
            roundReportResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    fun loadTodayExp() {
        call({
            NetManager.getApi().getNianfoTodayExp()
        }, {
            todayExpResult.value = successResult(it)
        }, {
            todayExpResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

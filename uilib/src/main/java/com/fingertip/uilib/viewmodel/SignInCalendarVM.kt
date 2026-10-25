package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.CalendarSimpleRsp
import com.fingertip.baselib.bean.SignInData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 签到日历页。进页面拉一次月历，签到成功后再拉一次刷新格子状态
 */
class SignInCalendarVM : TopVMImp() {

    val calendarResult = MutableLiveData<RequestResult<CalendarSimpleRsp>>()
    val signInDataResult = MutableLiveData<RequestResult<SignInData>>()
    val signInResult = MutableLiveData<RequestResult<String>>()

    /**
     * 月历：整月格子 + 今日详情。
     * year/month 传 0 时服务端按当月返回
     */
    fun loadCalendar(year: Int = 0, month: Int = 0) {
        call({
            NetManager.getApi().getMonthCalendar(year = year, month = month)
        }, {
            calendarResult.value = successResult(it)
        }, {
            calendarResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 签到信息（连签天数、下一档奖励），月历接口不带这些，单独拉一次
     */
    fun loadSignInData() {
        call({
            NetManager.getApi().getSignInData()
        }, {
            signInDataResult.value = successResult(it)
        }, {
            signInDataResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 签到。服务端只回成功失败，成功后由页面重新拉月历刷新
     */
    fun signIn() {
        call({
            NetManager.getApi().signIn()
        }, {
            signInResult.value = successResult(it)
        }, {
            signInResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

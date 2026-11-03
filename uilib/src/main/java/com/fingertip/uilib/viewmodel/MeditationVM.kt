package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 念佛计数。对应服务端 practice/meditationReport、practice/getMeditationTotal。
 *
 * 禅值结算在服务端：每满 108 声发 1 禅值，单日单佛号封顶 20 点。
 * 客户端只负责报数，**不做禅值计算也不做上限判断**——
 * 否则改个本地变量就能刷禅值。
 *
 * 上报节奏由页面控制（满 108 声或离开页面时才发），
 * 这里只提供"报一次"的动作，不做节流。
 */
class MeditationVM : TopVMImp() {

    //累计念佛总声数（全佛号合计）
    val totalResult = MutableLiveData<RequestResult<Int>>()

    //上报结果（成功不打扰用户，失败也不必弹窗）
    val reportResult = MutableLiveData<RequestResult<String>>()

    fun loadTotal() {
        call({
            NetManager.getApi().getMeditationTotal()
        }, {
            totalResult.value = successResult(it)
        }, {
            totalResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 上报一段计数
     * @param buddhaName 佛号名，服务端按「单日 + 单佛号」做上限
     * @param addCount 新增声数
     * @param addSeconds 这段计数花的时间（秒），只作统计
     */
    fun report(buddhaName: String, addCount: Int, addSeconds: Int) {
        if (addCount <= 0) return
        call({
            NetManager.getApi().reportMeditation(
                body = RequestBodyFactory.meditationReportBody(
                    buddhaName = buddhaName,
                    addCount = addCount,
                    addSeconds = addSeconds,
                    countDate = todayStr()
                )
            )
        }, {
            reportResult.value = successResult(it)
        }, {
            reportResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    private fun todayStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

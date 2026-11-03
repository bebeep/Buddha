package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.PracticeStatisticData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp
import java.util.TimeZone

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

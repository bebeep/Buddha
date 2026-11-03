package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.LampDetailData
import com.fingertip.baselib.bean.LampRecordData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 供灯状态。对应服务端 lamp/getStatus、lamp/getHistory。
 *
 * 只读——点灯/续燃一律走商城购买链路（dress/createOrder → 支付 → 验单），
 * 由服务端发货时调 lightLamp 落 lamp_record。
 * 这样"付钱"和"点灯"只有一条路径，不存在两套状态。
 *
 * 服务端查询前会先做一次惰性燃尽（过期灯置为已燃尽），
 * 所以客户端不需要自己按时间推算灯还在不在。
 */
class LampVM : TopVMImp() {

    //当前灯状态
    val statusResult = MutableLiveData<RequestResult<LampDetailData>>()

    //供灯历史（每条绑 IAP 订单号）
    val historyResult = MutableLiveData<RequestResult<List<LampRecordData>>>()

    fun loadStatus(buddhaId: Int = 0) {
        call({
            NetManager.getApi().getLampStatus(buddhaId = buddhaId)
        }, {
            statusResult.value = successResult(it)
        }, {
            statusResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * @param pageNum 从 0 起，每页 20 条
     */
    fun loadHistory(pageNum: Int = 0) {
        call({
            NetManager.getApi().getLampHistory(pageNum = pageNum)
        }, {
            historyResult.value = successResult(it)
        }, {
            historyResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

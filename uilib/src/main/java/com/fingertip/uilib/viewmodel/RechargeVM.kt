package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.CoinPackData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 功德币充值
 *
 * 链路与装饰商城 IAP 时代一致，只是订单换成充值单：
 *   getConfig → createOrder（拿 orderNo）→ 页面拉起商店支付 → verify（验单加币，返回余额）
 * 加币金额由服务端按订单里的商品 id 反查档位，客户端传什么都不影响。
 */
class RechargeVM : TopVMImp() {

    //档位列表
    val configResult = MutableLiveData<RequestResult<List<CoinPackData>>>()

    //建单结果，data 是内部订单号
    val orderResult = MutableLiveData<RequestResult<String>>()

    //验单到账结果，data 是最新功德币余额
    val verifyResult = MutableLiveData<RequestResult<String>>()

    fun loadConfig() {
        call({
            NetManager.getApi().getRechargeConfig()
        }, {
            configResult.value = successResult(it)
        }, {
            configResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    fun createOrder(configId: Int) {
        call({
            NetManager.getApi().createRechargeOrder(configId = configId)
        }, {
            orderResult.value = successResult(it)
        }, {
            orderResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    fun verify(orderNo: String, payOrderId: String) {
        call({
            NetManager.getApi().verifyRecharge(
                body = RequestBodyFactory.iapVerifyBody(
                    orderNo,
                    com.fingertip.baselib.net.NetProperty.PLATFORM,
                    payOrderId
                )
            )
        }, {
            verifyResult.value = successResult(it)
        }, {
            verifyResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }
}

package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 设置页：退出登录 / 注销账号
 *
 * 两个都是危险操作，UI 层必须先二次确认再调这里；
 * VM 只负责发请求和把结果抛回去，不自己清本地态——
 * 本地 session 的清理由页面在"服务端成功"之后做（服务端没清掉就清本地会留下一颗无效 session）。
 */
class SettingVM : TopVMImp() {

    val logoutResult = MutableLiveData<RequestResult<String>>()

    val deleteResult = MutableLiveData<RequestResult<String>>()

    fun logout() {
        call({
            NetManager.getApi().logout()
        }, {
            logoutResult.value = successResult(it)
        }, {
            logoutResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    fun deleteAccount() {
        call({
            NetManager.getApi().deleteAccount()
        }, {
            deleteResult.value = successResult(it)
        }, {
            deleteResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 恢复购买结果：data 是恢复件数
     */
    val restoreResult = MutableLiveData<RequestResult<Int>>()

    /**
     * 恢复购买：把平台侧的交易号交给服务端补发
     */
    fun restorePurchases(payOrderIds: List<String>) {
        call({
            NetManager.getApi().restorePurchase(body = RequestBodyFactory.restoreBody(payOrderIds))
        }, {
            restoreResult.value = successResult(it)
        }, {
            restoreResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

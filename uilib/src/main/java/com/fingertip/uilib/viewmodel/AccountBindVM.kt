package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.util.HashUtil
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 账号绑定（海外版）
 *
 * 调后端 /account/bindThird（loginType=1 邮箱 / 2 Google / 3 Apple）与 /account/setPassword。
 * 邮箱绑定后建议紧接着调 setPassword，否则绑了邮箱也无法用「邮箱 + 密码」登录。
 */
class AccountBindVM : TopVMImp() {

    val bindResult = MutableLiveData<RequestResult<String>>()
    val setPwdResult = MutableLiveData<RequestResult<String>>()

    /**
     * 绑定第三方账号 / 补绑邮箱
     * @param loginType 1 邮箱 2 Google 3 Apple
     */
    fun bindThird(loginType: Int, email: String = "", idToken: String = "") {
        call({
            NetManager.getApi()
                .bindThird(request = RequestBodyFactory.bindThirdBody(loginType, email, idToken))
        }, {
            bindResult.value = successResult(it)
        }, {
            bindResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = true)
    }

    /**
     * 设置 / 修改密码（邮箱登录用，密码客户端 MD5）
     */
    fun setPassword(pwd: String) {
        call({
            NetManager.getApi().setPassword(password = HashUtil.getMD5(pwd))
        }, {
            setPwdResult.value = successResult(it)
        }, {
            setPwdResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = true)
    }
}

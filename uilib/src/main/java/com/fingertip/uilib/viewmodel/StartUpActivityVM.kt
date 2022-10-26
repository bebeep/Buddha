package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.LoginRspData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.util.HashUtil
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp
import okhttp3.RequestBody

class StartUpActivityVM: TopVMImp() {

    val loginResult = MutableLiveData<RequestResult<LoginRspData>>()
    val bindAccountResult = MutableLiveData<RequestResult<String>>()

    fun login(account: String, pwd: String) {
        call({
            NetManager.getApi().login(request = RequestBodyFactory.loginBody(account, HashUtil.getMD5(pwd)))
        }, {
            loginResult.value = successResult(it)
        }, {
            loginResult.value = failResult(it.errorCode)
        }, showLoading = true, toastError = true)
    }

    fun bindAccount(requestResult: RequestBody) {
        call({
            NetManager.getApi().gd_bindAct(request = requestResult)
        }, {
            bindAccountResult.value = successResult(it)
        }, {
            bindAccountResult.value = failResult(it.errorCode)
        }, showLoading = true,toastError = true)
    }




}
package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.bean.LoginRspData
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.bean.VersionInfo
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.util.HashUtil
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

class MainVM: TopVMImp() {

    val loginResult = MutableLiveData<RequestResult<PersonData>>()
    val serverStatusResult = MutableLiveData<RequestResult<VersionInfo>>()



    fun login(account: String, pwd: String) {
        call({
            NetManager.getApi().login(request = RequestBodyFactory.loginBody(account, HashUtil.getMD5(pwd)))
        }, {
            loginResult.value = successResult(it)
        }, {
            loginResult.value = failResult(it.errorCode)
        }, showLoading = true, toastError = true)
    }




}
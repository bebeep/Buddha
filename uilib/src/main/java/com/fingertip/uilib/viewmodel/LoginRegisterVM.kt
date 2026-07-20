package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.bean.LoginRspData
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.util.HashUtil
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

class LoginRegisterVM: TopVMImp() {

    val loginResult = MutableLiveData<RequestResult<PersonData>>()


    fun login(phoneNum: String, pwd: String) {
        call({
            NetManager.getApi().login(request = RequestBodyFactory.loginBody(phoneNum, HashUtil.getMD5(pwd)))
        }, {
            SPUtils.getInstance(SPConstant.SP_ACCOUNT).put(SPConstant.SP_ACCOUNT_USERNAME, phoneNum)
            SPUtils.getInstance(SPConstant.SP_ACCOUNT).put(SPConstant.SP_ACCOUNT_PASSWORD, pwd)
            loginResult.value = successResult(it)
        }, {
            loginResult.value = failResult(it.errorCode)
        }, showLoading = true, toastError = true)
    }




}
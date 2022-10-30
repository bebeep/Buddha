package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.bean.LoginRspData
import com.fingertip.baselib.bean.ServerStatusEntity
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.constant.YHManager
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.util.HashUtil
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

class StartUpVM: TopVMImp() {

    val loginResult = MutableLiveData<RequestResult<LoginRspData>>()
    val serverStatusResult = MutableLiveData<RequestResult<ServerStatusEntity>>()


    /**
     * 检查服务器状态
     */
    fun checkServerStatus(){
        call({
            NetManager.getApi().checkServerStatus()
        }, {
            serverStatusResult.value = successResult(it)
            val username = SPUtils.getInstance(SPConstant.SP_ACCOUNT).getString(SPConstant.SP_ACCOUNT_USERNAME)
            val password = SPUtils.getInstance(SPConstant.SP_ACCOUNT).getString(SPConstant.SP_ACCOUNT_PASSWORD)
            if (it?.status == 1 && username.isNotEmpty() && password.isNotEmpty()){//情况正常，并且本地保存了账号密码 直接登录
                login(username,password)
            }
        }, {
            serverStatusResult.value = failResult(it.errorCode)
        }, showLoading = true, toastError = true)
    }

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
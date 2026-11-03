package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.bean.VersionInfo
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

class StartUpVM : TopVMImp() {

    val loginResult = MutableLiveData<RequestResult<PersonData>>()
    val serverStatusResult = MutableLiveData<RequestResult<VersionInfo>>()

    /**
     * 检查服务器状态
     */
    fun checkServerStatus() {
        call({
            NetManager.getApi().checkServerStatus()
        }, {
            serverStatusResult.value = successResult(it)
        }, {
            serverStatusResult.value = failResult(it.errorCode)
        }, showLoading = true, toastError = true)
    }

    /**
     * 冷启动自动登录：用本地 session 续期。
     *
     * 海外版不再保存账号密码（原来 SpUtils 里躺着明文密码，本身就是安全隐患），
     * 唯一的长效凭证就是 session。续期成功服务端会换发新 session，旧的同时作废。
     */
    fun refreshSession(session: String) {
        call({
            NetManager.getApi().refreshSession(session = session)
        }, {
            // 续期成功要把新 session 写回 SP，否则下次冷启动用的还是旧的那条
            it?.let { data ->
                SPUtils.getInstance(SPConstant.SP_ACCOUNT)
                    .put(SPConstant.SP_SESSION, data.latestSession ?: "")
            }
            loginResult.value = successResult(it)
        }, {
            loginResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

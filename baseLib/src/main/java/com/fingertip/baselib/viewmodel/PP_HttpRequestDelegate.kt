package com.fingertip.baselib.viewmodel

import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.bean.RequestRsp
import com.fingertip.baselib.loge
import com.fingertip.baselib.util.ToastUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Semaphore

class PP_HttpRequestDelegate(private val scope: CoroutineScope) {
    
    private val cName = "PP_HttpRequestDelegate"

    var goLoading = {}
    var stopLoading = {}

    fun <T> call(
        block: suspend CoroutineScope.() -> RequestRsp<T>,
        success: (T?) -> Unit,
        error: (PP_ApiHandler.ErrorInfo) -> Unit = {},
        toastError: Boolean = false,
        showLoading: Boolean = false
    ) {
        if (showLoading)
            goLoading()

        scope.launch {
            PP_ApiHandler.handleException(
                block = {
                    withContext(Dispatchers.IO) { block() }
                },
                success = { res ->
                    PP_ApiHandler.executeResponse(res) {
                        if (showLoading)
                            stopLoading()
                        success(it)
                    }
                },
                error = {
                    // 检查session是否有效
                    // 失效就重新登陆获取session，然后再重新执行本次请求
                    if (it.errorCode == -1234321) {
                        // 未知错误，重试。。。
                        loge(cName, "未知错误，重试。。。")
                        call(block, success, error, toastError, showLoading)
                    } else if (checkSessionInvalid(it)) {
                        if (toastError && it.errorCode != 1) {
                            if (it.errorCode == -12321) {
                                it.errorMsg?.let {msg ->
                                    ToastUtil.showMessage(msg)
                                }
                            } else {
                                showErrorToast(it.errorCode)
                            }
                        }
                        if (showLoading)
                            stopLoading()
                        error(it)
                    } else {
                        getSessionAndRetry(block, success, error, toastError)
                    }
                }
            )
        }
    }

    private val semaphore = Semaphore(1)

    fun showErrorToast(code: Int) {
        PP_ErrorUtils.showError(code)
    }

    fun checkSessionInvalid(errorInfo: PP_ApiHandler.ErrorInfo): Boolean {
        if (errorInfo.errorCode == PP_Constants.ResultCode.SessionInvalid ||
            errorInfo.errorCode == PP_Constants.ResultCode.SessionNotFound ||
            errorInfo.errorCode == PP_Constants.ResultCode.SessionExpired) {

            log(cName, "session过期了")
            return false
        }
        return true
    }

    fun <T> getSessionAndRetry(
        block: suspend CoroutineScope.() -> RequestRsp<T>,
        success: (T?) -> Unit,
        error: (PP_ApiHandler.ErrorInfo) -> Unit,
        toastError: Boolean
    ) {
       scope.launch(Dispatchers.IO) {
            try {
                semaphore.acquire()
                log(cName, "获得锁     ${Thread.currentThread()}")
                if (System.currentTimeMillis() - SPUtils.getInstance().getLong("session_time", 0) < 3000) {
                    log(cName, "上次设置session的时间距离小于三秒，直接请求")
                    call(block, success, error, toastError)
                    log(cName, "getSessionAndRetry   before unlock   ${Thread.currentThread()}")
                    semaphore.release()
                } else {
                    log(cName, "上次设置session的时间距离大于三秒，重新登陆获取session")
                    if (YHManager.isAccountLogin) {
                        log(cName, "getSessionAndRetry  已经登录过")
                        YHManager.yhData?.loginAccount?.let {
                            YHManager.yhData?.loginPassword?.let { it1 ->
                                val md5Password: String = HashUtil.getMD5(it1)
                                log(cName, "重新执行登陆")
                                call({
                                    PP_NetManager.getApi().login(request = PP_RequestBodyFactory.loginBody(it, md5Password))
                                }, {
                                    it?.let {
                                        saveLoginSession(it)
                                        call(block, success, error, toastError)
                                    }
                                    log(cName, "getSessionAndRetry   semaphore.release()   ${Thread.currentThread()}")
                                    semaphore.release()
                                }, {
                                    log(cName, "getSessionAndRetry   semaphore.release()   ${Thread.currentThread()}")
                                    semaphore.release()
                                })
                            }
                        }
                    } else if (YHManager.isTryLogin) {
                        log(cName, "getSessionAndRetry  已经I'm new，重新执行I'm new")
                        call({
                            PP_NetManager.getApi().shiwan(request = PP_RequestBodyFactory.tryBody())
                        }, {
                            it?.let {
                                saveLoginSession(it,true)
                                call(block, success, error, toastError)
                            }
                            log(cName, "getSessionAndRetry   semaphore.release()   ${Thread.currentThread()}")
                            semaphore.release()
                        }, {
                            log(cName, "getSessionAndRetry   semaphore.release()   ${Thread.currentThread()}")
                            semaphore.release()
                        })
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveLoginSession(response: PP_LoginRspData, isTryPlay:Boolean=false) {
        SPUtils.getInstance().put("session_time", System.currentTimeMillis())

        YHManager.setLoginData(response.apply {
            user?.apply {
                loginAccount = YHManager.yhData?.loginAccount
                loginPassword = YHManager.yhData?.loginPassword
            }
        }, isTryPlay)
    }
}
package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.util.HashUtil
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 登录 / 注册（海外版）
 *
 * 四条路：游客（设备号）、邮箱密码、Google、Apple（Apple 由 iOS 端调用，接口同一套）
 */
class LoginRegisterVM : TopVMImp() {

    /** 登录方式，与服务端 LOGIN_TYPE 对齐 */
    companion object {
        const val TYPE_GUEST = 0
        const val TYPE_EMAIL = 1
        const val TYPE_GOOGLE = 2
        const val TYPE_APPLE = 3
    }

    val loginResult = MutableLiveData<RequestResult<PersonData>>()
    val registerResult = MutableLiveData<RequestResult<PersonData>>()
    val checkEmailResult = MutableLiveData<RequestResult<String>>()

    /**
     * 邮箱登录
     */
    fun loginByEmail(email: String, pwd: String) {
        call({
            NetManager.getApi()
                .login(request = RequestBodyFactory.emailLoginBody(email, HashUtil.getMD5(pwd)))
        }, {
            saveLogin(it, TYPE_EMAIL, email)
            loginResult.value = successResult(it)
        }, {
            loginResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = true)
    }

    /**
     * 邮箱注册。成功即视为登录（服务端会直接下发 session）
     */
    fun register(email: String, pwd: String) {
        call({
            NetManager.getApi()
                .register(request = RequestBodyFactory.registerBody(email, HashUtil.getMD5(pwd)))
        }, {
            saveLogin(it, TYPE_EMAIL, email)
            registerResult.value = successResult(it)
        }, {
            registerResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = true)
    }

    /**
     * 第三方登录
     * @param loginType TYPE_GOOGLE / TYPE_APPLE
     */
    fun loginByThird(loginType: Int, idToken: String) {
        call({
            NetManager.getApi()
                .login(request = RequestBodyFactory.thirdLoginBody(loginType, idToken))
        }, {
            saveLogin(it, loginType, it?.email ?: "")
            loginResult.value = successResult(it)
        }, {
            loginResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = true)
    }

    /**
     * 游客登录。
     * 服务端按设备号创建/找回游客号，享正式账号全部功能；之后用 Google/Apple
     * 登录时服务端会把第三方凭证直接绑到这个游客号上，账号本身不换、数据不丢。
     */
    fun guestLogin() {
        call({
            NetManager.getApi().tryPlay(request = RequestBodyFactory.guestLoginBody())
        }, {
            saveLogin(it, TYPE_GUEST, "")
            loginResult.value = successResult(it)
        }, {
            loginResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = true)
    }

    /**
     * 邮箱是否可用（注册页边输边查，别等点了注册才报错）
     */
    fun checkEmail(email: String) {
        call({
            NetManager.getApi().checkEmail(email = email)
        }, {
            checkEmailResult.value = successResult(it)
        }, {
            checkEmailResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 统一的登录态落地。
     *
     * session 存两份是有意的：
     *   1 SP —— 冷启动时 StartUpActivity 用它续期，进程被回收也不丢
     *   2 GlobalConfig.userData —— setter 里会同步写 session 与整份用户信息
     */
    private fun saveLogin(data: PersonData?, loginType: Int, email: String) {
        if (data == null) return
        GlobalConfig.userData = data

        val sp = SPUtils.getInstance(SPConstant.SP_ACCOUNT)
        sp.put(SPConstant.SP_SESSION, data.latestSession ?: "")
        sp.put(SPConstant.SP_LOGIN_TYPE, loginType)
        if (email.isNotBlank()) {
            sp.put(SPConstant.SP_LAST_EMAIL, email.trim().lowercase())
        }
        // 旧版留在本机的明文密码必须清掉，别再躺着
        sp.remove(SPConstant.SP_ACCOUNT_PASSWORD)
        sp.remove(SPConstant.SP_ACCOUNT_USERNAME)
    }
}

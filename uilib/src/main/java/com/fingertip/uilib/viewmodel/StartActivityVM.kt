package com.fingertip.uilib.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.GsonUtils
import java.io.IOException
import java.lang.Exception

class StartActivityVM: TopVMImp() {

    val tryResult = MutableLiveData<PP_RequestResult<PP_LoginRspData>>()
    val loginResult = MutableLiveData<PP_RequestResult<PP_LoginRspData>>()
    val bindAccountResult = MutableLiveData<PP_RequestResult<String>>()

    fun shiwan() {
        call({
            PP_NetManager.getApi().shiwan(request = PP_RequestBodyFactory.tryBody())
        }, {
            tryResult.value = successResult(it)
        }, {
            tryResult.value = failResult(it.errorCode)
        }, showLoading = true, toastError = true)
    }

    fun login(account: String, pwd: String) {
        call({
            PP_NetManager.getApi().login(request = PP_RequestBodyFactory.loginBody(account, HashUtil.getMD5(pwd)))
        }, {
            loginResult.value = successResult(it)
        }, {
            loginResult.value = failResult(it.errorCode)
        }, showLoading = true, toastError = true)
    }

    fun bindAccount(requestResult: RequestBody) {
        call({
            PP_NetManager.getApi().gd_bindAct(request = requestResult)
        }, {
            bindAccountResult.value = successResult(it)
        }, {
            bindAccountResult.value = failResult(it.errorCode)
        }, showLoading = true,toastError = true)
    }


    var checkAfResult = MutableLiveData<PP_RequestResult<String>>()
    var updateResult = MutableLiveData<PP_RequestResult<PP_UpdataInfoData>>()

    fun checkAfAttribution() {
        call({
            PP_NetManager.getCheckServerApi().checkAfAttribution(
                deviceUUID = PP_DeviceIdUtils.getDeviceChannleID(TopApplication.instance),
                idfa = PP_DeviceIdUtils.getDeviceChannleID(TopApplication.instance),
                channelId = NetProperty.CHANNEL,
                clientVersion = NetProperty.VERSION
            )
        }, {
            checkAfResult.value = successResult(it)
        }, {
            log("${LOG_TAG}==checkaf_status",it.toString())
            checkAfResult.value = failResult(it.errorCode)
        })
    }

    fun checkUpdate() {
        call({
            PP_NetManager.getCheckServerApi().gd_checkUpdate(
                channelId = NetProperty.CHANNEL,
                clientVersion = NetProperty.VERSION
            )
        }, {
            updateResult.value = successResult(it)
        }, {
            log("${LOG_TAG}==check_update",it.toString())
            updateResult.value = failResult(it.errorCode)
        })
    }

    /**
     * 获取开屏广告配置
     */
    fun getConfigs(result:(success:Boolean,configBean:ServerConfigBean?)->Unit) {
        OkHttpClient().let {
            val request = Request.Builder()
                .url(if (BuildConfig.DEBUG)"https://sng-apps-configs.s3-us-west-2.amazonaws.com/149-Papaya-test/DefaultConfigs.json"
                else "https://sng-apps-configs.s3-us-west-2.amazonaws.com/149-AndroidZaki/DefaultConfigs.json")
                .build()
            it.newCall(request).enqueue(object :Callback{
                override fun onFailure(call: Call, e: IOException) {
                    result(false,null)
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        result(true,GsonUtils.fromJson(response.body?.string(),ServerConfigBean::class.java))
                    }catch (e:Exception){
                        result(false,null)
                    }
                }
            })
        }
    }

}
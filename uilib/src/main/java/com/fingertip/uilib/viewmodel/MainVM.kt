package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.bean.LoginRspData
import com.fingertip.baselib.bean.OssConfig
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.bean.VersionInfo
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.util.HashUtil
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

class MainVM: TopVMImp() {






    /**
     * 同步获取 OSS 配置（suspend 函数，需在协程中调用）
     */
    suspend fun getOssConfigSync(): OssConfig? {
        return try {
            val rsp = NetManager.getApi().getOssConfig()
            if (rsp.code == 0) rsp.data else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }




}
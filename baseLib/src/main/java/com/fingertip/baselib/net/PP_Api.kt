package com.fingertip.baselib.net

import com.fingertip.baselib.bean.LoginRspData
import com.fingertip.baselib.bean.RequestRsp
import com.fingertip.baselib.bean.YHManager
import com.fingertip.baselib.net.NetProperty
import okhttp3.RequestBody
import retrofit2.http.*

interface PP_Api {


    /**
     * 绑定账号
     */
    @POST()
    suspend fun gd_bindAct(
        @Url url: String = NetProperty.ACTBIND,
        @Body request: RequestBody?,
        @Query("session") session: String = YHManager.session
    ): RequestRsp<String?>
    /**
     * 登录
     */
    @POST()
    suspend fun login(
        @Url url: String = NetProperty.LOGIN,
        @Body request: RequestBody?
    ): RequestRsp<LoginRspData?>


}
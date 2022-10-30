package com.fingertip.baselib.net

import com.fingertip.baselib.bean.LoginRspData
import com.fingertip.baselib.bean.RequestRsp
import com.fingertip.baselib.bean.ServerStatusEntity
import com.fingertip.baselib.constant.YHManager
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiManager {



    /**
     * 检查服务器状态
     */
    @GET()
    suspend fun checkServerStatus(
        @Url url: String = NetProperty.CHECK_SERVER_STATUS,
        @Query("session") session: String = YHManager.session
    ): RequestRsp<ServerStatusEntity?>

    /**
     * 登录
     */
    @POST()
    suspend fun login(
        @Url url: String = NetProperty.LOGIN,
        @Body request: RequestBody?
    ): RequestRsp<LoginRspData?>

    /**
     * 移除黑名单
     */
    @POST()
    suspend fun removeHMD(
        @Url url: String = "",
        @Query("targetAccountId") targetAccountId: Int,
        @Query("session") session: String = YHManager.session
    ): RequestRsp<String?>

    /**
     * 移除黑名单
     */
    @POST()
    suspend fun upAvatar(
        @Url url: String = "",
        @Query("imageUrl") imageUrl: String,
        @Query("session") session: String = YHManager.session
    ): RequestRsp<String?>
}
package com.fingertip.baselib.net

import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.bean.RequestRsp
import com.fingertip.baselib.bean.VersionInfo
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.util.DeviceIdUtils
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiManager {



    /**
     * 检查服务器状态
     */
    @POST()
    suspend fun checkServerStatus(
        @Url url: String = NetProperty.CHECK_SERVER_STATUS,
        @Query("channelId") channelId: Int = NetProperty.CHANNEL,
        @Query("appVersion") appVersion: String = DeviceIdUtils.getVersionName()
    ): RequestRsp<VersionInfo?>

    /**
     * 登录
     */
    @POST()
    suspend fun login(
        @Url url: String = NetProperty.LOGIN,
        @Body request: RequestBody?
    ): RequestRsp<PersonData?>

    /**
     * 移除黑名单
     */
    @POST()
    suspend fun removeHMD(
        @Url url: String = "",
        @Query("targetAccountId") targetAccountId: Int,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<String?>

    /**
     * 移除黑名单
     */
    @POST()
    suspend fun upAvatar(
        @Url url: String = "",
        @Query("imageUrl") imageUrl: String,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<String?>
}
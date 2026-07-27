package com.fingertip.baselib.net

import com.fingertip.baselib.bean.OssConfig
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
     * 获取oss配置
     */
    @GET()
    suspend fun getOssConfig(
        @Url url: String = NetProperty.GET_OSS_CONFIG,
        @Query("session") session: String = GlobalConfig.session,
    ): RequestRsp<OssConfig?>

    /**
     * 登录
     */
    @POST()
    suspend fun login(
        @Url url: String = NetProperty.LOGIN,
        @Body request: RequestBody?
    ): RequestRsp<PersonData?>

    /**
     * 发布动态
     */
    @POST()
    suspend fun postMoment(
        @Url url: String = NetProperty.POST_MOMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody?
    ): RequestRsp<String>

}
package com.fingertip.baselib.net

import com.fingertip.baseLib.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


/**
 * 发起接口请求工具类
 */
object PP_NetManager {

    private var api: PP_Api? = null

    fun getApi(isNosave: Boolean = false): PP_Api {
        synchronized(PP_NetManager::class.java) {
            if (isNosave){
                return getRetrofit(NetProperty.BASE_URL).create(PP_Api::class.java)
            }
            if (api == null) {
                api = getRetrofit(NetProperty.BASE_URL).create(PP_Api::class.java)
            }

        }
        return api!!
    }



    private fun getOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        if (BuildConfig.DEBUG) {
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            loggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        }

        val builder = OkHttpClient.Builder()
            .readTimeout(30000, TimeUnit.MILLISECONDS)
            .connectTimeout(30000, TimeUnit.MILLISECONDS)
            .addInterceptor(loggingInterceptor)

        return builder.build()
    }

    var mOkHttpClient:OkHttpClient?=null
    private fun getRetrofit(baseUrl: String): Retrofit {
        if(mOkHttpClient == null ) mOkHttpClient = getOkHttpClient()
        return Retrofit.Builder()
            .client(mOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl)
            .build()
    }
}
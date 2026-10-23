package com.fingertip.baselib.viewmodel

/**
 * 请求结果。errorCode 是服务端 R.code（网络异常是本地负数），
 * errorMsg 是服务端 R.msg 原文，失败时直接拿来展示，不要在页面里另写一套文案
 */
data class RequestResult<out T>(
    val success: Boolean,
    val data: T?,
    val errorCode: Int = -1,
    val errorMsg: String? = null
) {

    companion object{
        const val RESULT_OK = 0
        const val RESULT_FAIL = 1
    }
}

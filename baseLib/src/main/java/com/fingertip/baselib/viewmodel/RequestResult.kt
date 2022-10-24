package com.fingertip.baselib.viewmodel

data class RequestResult<out T>(val success: Boolean, val data: T?, val errorCode: Int = -1) {

    companion object{
        const val RESULT_OK = 0
        const val RESULT_FAIL = 1
    }
}
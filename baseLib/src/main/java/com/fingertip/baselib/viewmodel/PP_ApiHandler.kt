package com.fingertip.baselib.viewmodel


import com.fingertip.baseLib.R
import com.fingertip.baselib.bean.RequestRsp
import com.fingertip.baselib.log
import com.fingertip.baselib.loge
import com.fingertip.baselib.top.TopApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.json.JSONException
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object PP_ApiHandler {
    suspend fun<T> handleException(
        block: suspend CoroutineScope.() -> RequestRsp<T>,
        success: suspend CoroutineScope.(RequestRsp<T>) -> Unit,
        error: suspend CoroutineScope.(ErrorInfo) -> Unit
    ) {
        coroutineScope {
            try {
                success(block())
            } catch (e: Throwable) {
                loge("ApiHandler", "catch error")
                e.printStackTrace()
                error(parseException(e))
            }
        }
    }

    private fun parseException(e: Throwable): ErrorInfo {
        log("ApiHandler", "parseException -> $e    ${e.message}")
        return when (e) {
            is ResponseThrowable -> ErrorInfo(e.errorCode, e.errorMsg)
            is SocketTimeoutException -> ErrorInfo(-12321, errorMsg = TopApplication.instance.resources.getString(
                R.string.uselib_net1))
            is HttpException -> {
                when (e.code()) {
                    404 -> ErrorInfo(-12321, TopApplication.instance.resources.getString(R.string.uselib_net1))
                    500 -> ErrorInfo(-12321, TopApplication.instance.resources.getString(R.string.uselib_net1))
                    else -> ErrorInfo(-12321, TopApplication.instance.resources.getString(R.string.uselib_net1))
                }
            }
            is JSONException -> ErrorInfo(-12321, errorMsg = TopApplication.instance.resources.getString(R.string.uselib_net1))
            is UnknownHostException -> ErrorInfo(-12321, errorMsg = TopApplication.instance.resources.getString(R.string.uselib_net2))
            else ->
                if (e.message?.contains(TopApplication.instance.resources.getString(R.string.uselib_net3), true) == true)
                    ErrorInfo(-1234321, errorMsg = TopApplication.instance.resources.getString(R.string.uselib_net4))
                else if (e.message?.contains(TopApplication.instance.resources.getString(R.string.uselib_net5), true) == true)
                    ErrorInfo(-1234321, errorMsg = TopApplication.instance.resources.getString(R.string.uselib_net4))
                else ErrorInfo(-12321, errorMsg = TopApplication.instance.resources.getString(R.string.uselib_net1))
        }
    }

    suspend fun <T> executeResponse(
        response: RequestRsp<T>,
        success: suspend CoroutineScope.(T?) -> Unit
    ) {
        coroutineScope {
            if (response.retCode == 0)  success(response.data)
            else if (response.retCode == PP_Constants.ResultCode.AccountBlocked){
                EventBusProxy.post(MessageEvent(PP_EventBusCons.EVENT_BLOCKED,response.message))
            }
            else throw ResponseThrowable(response.retCode, response.message)
        }
    }

    class ResponseThrowable(val errorCode: Int, val errorMsg: String?): Throwable()

    class ErrorInfo(val errorCode: Int = 1, val errorMsg: String? = ""
    ) {
        override fun toString(): String {
            return "ErrorInfo(errorCode=$errorCode, errorMsg=$errorMsg)"
        }
    }
}
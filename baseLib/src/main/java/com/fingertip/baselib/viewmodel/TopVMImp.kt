package com.fingertip.baselib.viewmodel

import androidx.lifecycle.viewModelScope
import com.fingertip.baselib.bean.RequestRsp
import kotlinx.coroutines.CoroutineScope

open class TopVMImp: TopViewModel() {

    private val delegate by lazy { HttpRequestDelegate(viewModelScope).apply {
        goLoading = { this@TopVMImp.goLoading() }
        stopLoading = { this@TopVMImp.stopLoading() }
    } }

    fun <T> call(
        block: suspend CoroutineScope.() -> RequestRsp<T>,
        success: (T?) -> Unit,
        error: (ApiHandler.ErrorInfo) -> Unit = {},
        toastError: Boolean = false,
        showLoading: Boolean = false
    ) {
        delegate.call(block, success, error, toastError, showLoading)
    }
}
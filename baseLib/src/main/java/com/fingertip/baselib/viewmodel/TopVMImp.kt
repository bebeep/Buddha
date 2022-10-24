package com.lzlz.uselib.viewmodel

import androidx.lifecycle.viewModelScope
import com.fingertip.baselib.viewmodel.PP_ApiHandler
import com.lzlz.toplib.viewmodel.TopViewModel
import com.lzlz.uselib.bianjava.RequestRsp

import kotlinx.coroutines.CoroutineScope

open class TopVMImp: TopViewModel() {

    private val delegate by lazy { PP_HttpRequestDelegate(viewModelScope).apply {
        goLoading = { this@TopVMImp.goLoading() }
        stopLoading = { this@TopVMImp.stopLoading() }
    } }

    fun <T> call(
        block: suspend CoroutineScope.() -> RequestRsp<T>,
        success: (T?) -> Unit,
        error: (PP_ApiHandler.ErrorInfo) -> Unit = {},
        toastError: Boolean = false,
        showLoading: Boolean = false
    ) {
        delegate.call(block, success, error, toastError, showLoading)
    }
}
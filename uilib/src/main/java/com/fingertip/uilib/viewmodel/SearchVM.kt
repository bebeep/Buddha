package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.SearchResultEntity
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 搜索页：一次请求同时拿回匹配的佛友和动态。
 * 搜索是高频输入触发，失败静默交给页面兜底，不弹 toast 打扰。
 */
class SearchVM : TopVMImp() {

    val searchResult = MutableLiveData<RequestResult<SearchResultEntity>>()

    fun search(keyword: String) {
        call({
            NetManager.getApi().search(keyword = keyword.trim())
        }, {
            searchResult.value = successResult(it)
        }, {
            searchResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

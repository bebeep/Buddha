package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.BookSubjectEntity
import com.fingertip.baselib.bean.BookTypeConfig
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp


class BookshelfVM: TopVMImp() {

    val bookTypeResult = MutableLiveData<RequestResult<List<BookTypeConfig>>>()
    val bookSubjectResult = MutableLiveData<RequestResult<List<BookSubjectEntity>>>()


    fun getBookTypeList() {
        call({
            NetManager.getApi().getBookTypeList()
        }, {
            bookTypeResult.value = successResult(it)
        }, {
            bookTypeResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getBookThemeList() {
        call({
            NetManager.getApi().getBookThemeList()
        }, {
            bookSubjectResult.value = successResult(it)
        }, {
            bookSubjectResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getBookByTheme() {
        call({
            NetManager.getApi().getBookByTheme()
        }, {
            bookSubjectResult.value = successResult(it)
        }, {
            bookSubjectResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }
}
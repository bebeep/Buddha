package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.BookComment
import com.fingertip.baselib.bean.BookThemeEntity
import com.fingertip.baselib.bean.BookTypeConfig
import com.fingertip.baselib.bean.BuddhaBook
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp


class BookshelfVM: TopVMImp() {

    val bookTypeResult = MutableLiveData<RequestResult<List<BookTypeConfig>>>()
    val bookSubjectResult = MutableLiveData<RequestResult<List<BookThemeEntity>>>()
    val bookListResult = MutableLiveData<RequestResult<List<BuddhaBook>>>()

    val bookInfoResult = MutableLiveData<RequestResult<BuddhaBook>>()
    val bookCommentResult = MutableLiveData<RequestResult<List<BookComment>>>()


    fun getBookTypeList() {
        call({
            NetManager.getApi().getBookTypeList()
        }, {
            bookTypeResult.value = successResult(it)
        }, {
            bookTypeResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getBookSubjectList() {
        call({
            NetManager.getApi().getBookThemeList()
        }, {
            bookSubjectResult.value = successResult(it)
        }, {
            bookSubjectResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getBookByTheme(themeId: Int,currPage: Int) {
        call({
            NetManager.getApi().getBookByTheme(themeId = themeId, currPage = currPage)
        }, {
            bookListResult.value = successResult(it)
        }, {
            bookListResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getBookByType(typeId: Int,currPage: Int) {
        call({
            NetManager.getApi().getBookByType(typeId = typeId, currPage = currPage)
        }, {
            bookListResult.value = successResult(it)
        }, {
            bookListResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getBookInShelf(currPage: Int) {
        call({
            NetManager.getApi().getBookInShelf(currPage = currPage)
        }, {
            bookListResult.value = successResult(it)
        }, {
            bookListResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getBookInfoByBookId(bookId: Int) {
        call({
            NetManager.getApi().getBookInfoByBookId(bookId = bookId)
        }, {
            bookInfoResult.value = successResult(it)
        }, {
            bookInfoResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getBookComment(bookId: Int,currPage: Int) {
        call({
            NetManager.getApi().getBookComment(bookId = bookId, currPage = currPage)
        }, {
            bookCommentResult.value = successResult(it)
        }, {
            bookCommentResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }
}
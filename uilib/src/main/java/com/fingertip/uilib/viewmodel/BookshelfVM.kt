package com.fingertip.uilib.viewmodel

import android.R
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
    fun likeBookComment(bookId: Int,commentId: Int,like: Boolean,onResult: (success: Boolean) -> Unit) {
        call({
            if (like) NetManager.getApi().likeBookComment(bookId = bookId, commentId = commentId)
            else NetManager.getApi().unLikeBookComment(bookId = bookId, commentId = commentId)
        }, {
            onResult(true)
        }, {
            onResult(false)
        }, showLoading = false, toastError = false)
    }

    /**
     * 加入书架
     */
    fun addToBookShelf(bookId: Int, onResult: (success: Boolean) -> Unit) {
        call({
            NetManager.getApi().addToBookShelf(bookId = bookId)
        }, {
            onResult(true)
        }, {
            onResult(false)
        }, showLoading = false, toastError = false)
    }

    /**
     * 移出书架
     */
    fun removeFromBookShelf(bookId: Int, onResult: (success: Boolean) -> Unit) {
        call({
            NetManager.getApi().removeFromBookShelf(bookId = bookId)
        }, {
            onResult(true)
        }, {
            onResult(false)
        }, showLoading = false, toastError = false)
    }

    /**
     * 添加书籍评论
     */
    fun addBookComment(bookId: Int, textContent: String, onResult: (success: Boolean,comment:BookComment?) -> Unit) {
        call({
            NetManager.getApi().addBookComment(bookId = bookId, textContent = textContent)
        }, {
            onResult(true,it)
        }, {
            onResult(false,null)
        }, showLoading = false, toastError = false)
    }

    /**
     * 删除书籍评论
     */
    fun removeBookComment(commentId: Int, onResult: (success: Boolean) -> Unit) {
        call({
            NetManager.getApi().removeBookComment(commentId = commentId)
        }, {
            onResult(true)
        }, {
            onResult(false)
        }, showLoading = false, toastError = false)
    }
}
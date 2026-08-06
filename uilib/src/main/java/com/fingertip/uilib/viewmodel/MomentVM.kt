package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

class MomentVM:TopVMImp() {

    val postMomentResult = MutableLiveData<RequestResult<String>>()
    val momentListResult = MutableLiveData<RequestResult<List<MomentEntity>>>()
    val momentDetailsResult = MutableLiveData<RequestResult<MomentEntity>>()

    val momentCommentListResult = MutableLiveData<RequestResult<List<CommentEntity>>>()

    val momentCommentDetailsResult = MutableLiveData<RequestResult<List<CommentEntity>>>()
    val commentResult = MutableLiveData<RequestResult<CommentEntity>>()


    fun postMoment(textContent: String = "",
                   videoUrl: String = "",
                   videoCover: String = "",
                   imageUrl: String = "",
                   videoDuration: Int = 0,
                   videoWidth: Int = 0,
                   videoHeight: Int = 0,
                   isAnonymous: Boolean = false,
                   location: String = "",
                   remindAccountIds: List<Int> = emptyList()) {
        call({
            NetManager.getApi().postMoment(request = RequestBodyFactory.postMomentBody(textContent,
                videoUrl,videoCover,imageUrl,videoDuration,videoWidth,videoHeight,
                isAnonymous,location,remindAccountIds))
        }, {
            postMomentResult.value = successResult(it)
        }, {
            postMomentResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getMomentList(pageCount: Int,momentType: Int) {
        call({
            when(momentType){
                1 -> NetManager.getApi().getMomentListFollowed(pageCount = pageCount)
                else -> NetManager.getApi().getMomentListHot(pageCount = pageCount)
            }
        }, {
            momentListResult.value = successResult(it)
        }, {
            momentListResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getMomentDetails(momentId: Int) {
        call({
            NetManager.getApi().getMomentDetails(momentId= momentId)
        }, {
            momentDetailsResult.value = successResult(it)
        }, {
            momentDetailsResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getCommentList(momentId: Int,pageCount: Int) {
        call({
            NetManager.getApi().getCommentByMomentId(momentId = momentId, pageCount = pageCount)
        }, {
            momentCommentListResult.value = successResult(it)
        }, {
            momentCommentListResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    fun getCommentDetails(commentId: Int,pageCount: Int) {
        call({
            NetManager.getApi().getCommentDetails(commentId = commentId, pageCount = pageCount)
        }, {
            momentCommentDetailsResult.value = successResult(it)
        }, {
            momentCommentDetailsResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }


    fun commentMoment(momentId: Int,parentCommentId: Int,targetCommentId: Int,textContent: String) {
        call({
            NetManager.getApi().commentMoment(request = RequestBodyFactory.postMomentBody(momentId,parentCommentId,targetCommentId,textContent))
        }, {
            commentResult.value = successResult(it)
        }, {
            commentResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    fun viewMoment(momentIds: List<Int>) {
        call({
            NetManager.getApi().viewMoment(request = RequestBodyFactory.viewMomentBody(momentIds))
        }, {
        }, {
        }, showLoading = false, toastError = false)
    }

    fun likeMoment(momentId: Int,like: Boolean,likeResult:(like:Boolean,success:Boolean)->Unit) {
        call({
            if (like)
                NetManager.getApi().likeMoment(momentId = momentId)
            else NetManager.getApi().unLikeMoment(momentId = momentId)
        }, {
            likeResult(like, true)
        }, {
            likeResult(like, false)
        }, showLoading = false, toastError = false)
    }

    fun likeMomentComment(commentId: Int,like: Boolean) {
        call({
            if (like)
                NetManager.getApi().likeMomentComment(commentId = commentId)
            else NetManager.getApi().unLikeMomentComment(commentId = commentId)
        }, {
        }, {
        }, showLoading = false, toastError = false)
    }

}
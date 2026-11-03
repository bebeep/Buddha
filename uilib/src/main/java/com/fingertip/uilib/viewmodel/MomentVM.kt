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
                   isCheckIn: Boolean = false,
                   location: String = "",
                   remindAccountIds: List<Int> = emptyList()) {
        call({
            NetManager.getApi().postMoment(request = RequestBodyFactory.postMomentBody(textContent,
                videoUrl,videoCover,imageUrl,videoDuration,videoWidth,videoHeight,
                isAnonymous,location,remindAccountIds,checkIn = isCheckIn))
        }, {
            postMomentResult.value = successResult(it)
        }, {
            postMomentResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    /**
     * 动态列表：0 推荐(热) 1 关注 2 共修打卡 3 最新
     */
    fun getMomentList(pageCount: Int,listType: Int) {
        call({
            when(listType){
                1 -> NetManager.getApi().getMomentListFollowed(pageCount = pageCount)
                2 -> NetManager.getApi().getMomentListCheckIn(pageCount = pageCount)
                3 -> NetManager.getApi().getMomentListNew(pageCount = pageCount)
                else -> NetManager.getApi().getMomentListHot(pageCount = pageCount)
            }
        }, {
            momentListResult.value = successResult(it)
        }, {
            momentListResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    //我的感悟 / 他人主页的动态列表
    val momentByAccountResult = MutableLiveData<RequestResult<List<MomentEntity>>>()
    val deleteMomentResult = MutableLiveData<RequestResult<String>>()

    /**
     * 某人的动态列表
     * @param targetAccountId 传 0 查自己
     */
    fun getMomentListByAccount(targetAccountId: Int, pageCount: Int) {
        call({
            NetManager.getApi().getMomentListByAccount(
                targetAccountId = targetAccountId, pageCount = pageCount
            )
        }, {
            momentByAccountResult.value = successResult(it)
        }, {
            momentByAccountResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 删除自己的动态
     */
    fun deleteMoment(momentId: Int) {
        call({
            NetManager.getApi().deleteMoment(momentId = momentId)
        }, {
            deleteMomentResult.value = successResult(it)
        }, {
            deleteMomentResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
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
            momentCommentListResult.value = failResult(it.errorCode, it.errorMsg)
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
            commentResult.value = failResult(it.errorCode, it.errorMsg)
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

    //举报结果（UGC 合规：举报成功给个轻提示即可，不暴露处理细节）
    val reportResult = MutableLiveData<RequestResult<String>>()

    /**
     * 举报动态或评论
     * @param targetAccountId 被举报内容的作者
     * @param reportType 1动态 2评论
     * @param tag 举报原因，见 ReportDialog.TAG_*
     * @param content 附加说明，一期留空
     */
    fun report(targetAccountId: Int, reportType: Int, tag: String, content: String = "") {
        call({
            NetManager.getApi().report(
                body = RequestBodyFactory.reportBody(targetAccountId, reportType, tag, content)
            )
        }, {
            reportResult.value = successResult(it)
        }, {
            reportResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

}
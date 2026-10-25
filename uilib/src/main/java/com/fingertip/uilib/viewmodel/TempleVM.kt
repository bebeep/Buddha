package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.TempleBean
import com.fingertip.baselib.bean.TempleCommentBean
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 寺庙。对应 temple 一组接口：列表/详情/推荐/评论/评论列表
 * 经纬度：设备定位服务没接，先统一传0，服务端会返回不带距离的数据
 */
class TempleVM : TopVMImp() {

    val listResult = MutableLiveData<RequestResult<List<TempleBean>>>()
    val detailResult = MutableLiveData<RequestResult<TempleBean>>()
    val recommendResult = MutableLiveData<RequestResult<String>>()
    val commentResult = MutableLiveData<RequestResult<String>>()
    val commentListResult = MutableLiveData<RequestResult<List<TempleCommentBean>>>()

    /**
     * 寺庙列表，currPage 从 0 开始
     */
    fun loadList(currPage: Int) {
        call({
            NetManager.getApi().getTempleList(currPage = currPage)
        }, {
            listResult.value = successResult(it)
        }, {
            listResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = currPage == 0, toastError = false)
    }

    /**
     * 寺庙详情
     */
    fun loadDetail(templeId: Int) {
        call({
            NetManager.getApi().getTempleInfo(templeId = templeId)
        }, {
            detailResult.value = successResult(it)
        }, {
            detailResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 提交推荐，recommendStatus 1推荐 2不推荐
     */
    fun recommend(templeId: Int, recommendStatus: Int) {
        call({
            NetManager.getApi().postRecommendStatus(templeId = templeId, recommendStatus = recommendStatus)
        }, {
            recommendResult.value = successResult(it)
        }, {
            recommendResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 发表评论
     */
    fun postComment(templeId: Int, comment: String) {
        call({
            NetManager.getApi().postTempleComment(templeId = templeId, comment = comment)
        }, {
            commentResult.value = successResult(it)
        }, {
            commentResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 评论列表，pageNum 从 0 开始
     */
    fun loadComments(templeId: Int, pageNum: Int) {
        call({
            NetManager.getApi().getTempleComment(templeId = templeId, pageNum = pageNum)
        }, {
            commentListResult.value = successResult(it)
        }, {
            commentListResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = pageNum == 0, toastError = false)
    }
}

package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.BlessRecordBean
import com.fingertip.baselib.bean.WishRecordBean
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 许愿树。
 * - 许愿树列表：进页面拉一次，看完/祝福过的愿望服务端会记已读，下次拉取自动换一批
 * - 我的愿望/我的祝福：pageNum 从 0 开始分页，PAGE_SIZE 由服务端 GlobalConfig.PAGE_SIZE 决定
 */
class WishVM : TopVMImp() {

    companion object {
        //我的列表每页条数，与服务端 GlobalConfig.PAGE_SIZE 保持一致
        const val PAGE_SIZE = 10
    }

    val wishTreeResult = MutableLiveData<RequestResult<List<WishRecordBean>>>()
    val wishTagsResult = MutableLiveData<RequestResult<List<String>>>()
    val postWishResult = MutableLiveData<RequestResult<String>>()
    val deleteWishResult = MutableLiveData<RequestResult<String>>()
    val blessResult = MutableLiveData<RequestResult<String>>()
    val myWishResult = MutableLiveData<RequestResult<List<WishRecordBean>>>()
    val myBlessResult = MutableLiveData<RequestResult<List<BlessRecordBean>>>()
    val wishBlessListResult = MutableLiveData<RequestResult<List<BlessRecordBean>>>()

    /**
     * 许愿树
     */
    fun loadWishTree() {
        call({
            NetManager.getApi().getWishingTree()
        }, {
            wishTreeResult.value = successResult(it)
        }, {
            wishTreeResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 许愿文本示例，用于编辑页的推荐标签
     */
    fun loadWishTags() {
        call({
            NetManager.getApi().getWishingTag()
        }, {
            wishTagsResult.value = successResult(it)
        }, {
            wishTagsResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 发布愿望
     */
    fun postWishing(wishContent: String) {
        call({
            NetManager.getApi().postWishing(request = RequestBodyFactory.wishBody(wishContent))
        }, {
            postWishResult.value = successResult(it)
        }, {
            postWishResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 删除自己的愿望
     */
    fun deleteWishing(wishingId: Int) {
        call({
            NetManager.getApi().deleteWishing(wishingId = wishingId)
        }, {
            deleteWishResult.value = successResult(it)
        }, {
            deleteWishResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 为愿望送祝福
     */
    fun blessWishing(wishingId: Int, blessContent: String) {
        call({
            NetManager.getApi().blessWishing(request = RequestBodyFactory.blessBody(wishingId, blessContent))
        }, {
            blessResult.value = successResult(it)
        }, {
            blessResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 愿望已读，服务端会把这些愿望从我的许愿树里排除一段时间
     */
    fun readWishing(wishingIds: List<Int>) {
        if (wishingIds.isEmpty()) return
        launchUI {
            runCatching {
                NetManager.getApi().readWishing(request = RequestBodyFactory.wishReadBody(wishingIds))
            }
        }
    }

    /**
     * 我的愿望，pageNum 从 0 开始
     */
    fun loadMyWishes(pageNum: Int) {
        call({
            NetManager.getApi().getMyWishingRecord(pageNum = pageNum)
        }, {
            myWishResult.value = successResult(it)
        }, {
            myWishResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = pageNum == 0, toastError = false)
    }

    /**
     * 我的祝福，pageNum 从 0 开始
     */
    fun loadMyBless(pageNum: Int) {
        call({
            NetManager.getApi().getMyBlessRecord(pageNum = pageNum)
        }, {
            myBlessResult.value = successResult(it)
        }, {
            myBlessResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = pageNum == 0, toastError = false)
    }

    /**
     * 某条愿望收到的祝福列表，pageNum 从 0 开始
     */
    fun loadWishBlessList(wishingId: Int, pageNum: Int) {
        call({
            NetManager.getApi().getBlessRecordByWishingId(wishingId = wishingId, pageNum = pageNum)
        }, {
            wishBlessListResult.value = successResult(it)
        }, {
            wishBlessListResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = pageNum == 0, toastError = false)
    }
}

package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.DeedBean
import com.fingertip.baselib.bean.DeedDonateBean
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 行善。对应 goodDeed 一组接口：
 * - 列表/详情/发布/删除/捐赠/分享/捐赠记录/我发布的/我捐赠的
 * - 分页 pageNum 从 0 开始
 */
class GoodDeedVM : TopVMImp() {

    val listResult = MutableLiveData<RequestResult<List<DeedBean>>>()
    val detailResult = MutableLiveData<RequestResult<DeedBean>>()
    val postResult = MutableLiveData<RequestResult<String>>()
    val donateResult = MutableLiveData<RequestResult<String>>()
    val shareResult = MutableLiveData<RequestResult<String>>()
    val donateListResult = MutableLiveData<RequestResult<List<DeedDonateBean>>>()
    val myDeedResult = MutableLiveData<RequestResult<List<DeedBean>>>()
    val myDonateResult = MutableLiveData<RequestResult<List<DeedBean>>>()

    /**
     * 善行列表，currPage 从 0 开始；category 传空为全部
     */
    fun loadList(currPage: Int, category: String?) {
        call({
            NetManager.getApi().getGoodDeedList(currPage = currPage, category = category)
        }, {
            listResult.value = successResult(it)
        }, {
            listResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = currPage == 0, toastError = false)
    }

    /**
     * 善行详情（含我的捐赠统计）
     */
    fun loadDetail(deedId: Int) {
        call({
            NetManager.getApi().getGoodDeedInfo(deedId = deedId)
        }, {
            detailResult.value = successResult(it)
        }, {
            detailResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 发布求助
     */
    fun postDeed(title: String, content: String, category: String?, isUrgent: Int, targetCoins: Int) {
        call({
            NetManager.getApi().postGoodDeed(
                title = title, content = content, category = category,
                isUrgent = isUrgent, targetCoins = targetCoins
            )
        }, {
            postResult.value = successResult(it)
        }, {
            postResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 捐赠功德币
     */
    fun donate(deedId: Int, coins: Int, message: String?) {
        call({
            NetManager.getApi().donateGoodDeed(deedId = deedId, coins = coins, message = message)
        }, {
            donateResult.value = successResult(it)
        }, {
            donateResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 分享助力
     */
    fun share(deedId: Int) {
        call({
            NetManager.getApi().shareGoodDeed(deedId = deedId)
        }, {
            shareResult.value = successResult(it)
        }, {
            shareResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 善行捐赠记录，pageNum 从 0 开始
     */
    fun loadDonateList(deedId: Int, pageNum: Int) {
        call({
            NetManager.getApi().getDeedDonateRecord(deedId = deedId, pageNum = pageNum)
        }, {
            donateListResult.value = successResult(it)
        }, {
            donateListResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 我发布的求助，pageNum 从 0 开始
     */
    fun loadMyDeeds(pageNum: Int) {
        call({
            NetManager.getApi().getMyDeedRecord(pageNum = pageNum)
        }, {
            myDeedResult.value = successResult(it)
        }, {
            myDeedResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = pageNum == 0, toastError = false)
    }

    /**
     * 我捐赠的善行，pageNum 从 0 开始
     */
    fun loadMyDonateDeeds(pageNum: Int) {
        call({
            NetManager.getApi().getMyDonateDeedRecord(pageNum = pageNum)
        }, {
            myDonateResult.value = successResult(it)
        }, {
            myDonateResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = pageNum == 0, toastError = false)
    }
}

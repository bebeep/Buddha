package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.PrayingBean
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 祈福。对应 blessing 一组接口：
 * - 福卡墙：随机20张，祝福过的自动排除
 * - 今日祝福状态：data 为今日已祝福次数（上限5）
 */
class BlessingVM : TopVMImp() {

    val wallResult = MutableLiveData<RequestResult<List<PrayingBean>>>()
    val allPrayingResult = MutableLiveData<RequestResult<List<PrayingBean>>>()
    val myPrayingResult = MutableLiveData<RequestResult<List<PrayingBean>>>()
    val statusResult = MutableLiveData<RequestResult<Int>>()
    val postResult = MutableLiveData<RequestResult<String>>()
    val blessResult = MutableLiveData<RequestResult<String>>()
    val deleteResult = MutableLiveData<RequestResult<String>>()

    /**
     * 福卡墙
     */
    fun loadWall() {
        call({
            NetManager.getApi().getPrayingWall()
        }, {
            wallResult.value = successResult(it)
        }, {
            wallResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 所有祈福，pageNum 从 0 开始
     */
    fun loadAllPraying(pageNum: Int) {
        call({
            NetManager.getApi().getAllPrayingRecord(pageNum = pageNum)
        }, {
            allPrayingResult.value = successResult(it)
        }, {
            allPrayingResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = pageNum == 0, toastError = false)
    }

    /**
     * 我的祈福，pageNum 从 0 开始
     */
    fun loadMyPraying(pageNum: Int) {
        call({
            NetManager.getApi().getMyPrayingRecord(pageNum = pageNum)
        }, {
            myPrayingResult.value = successResult(it)
        }, {
            myPrayingResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = pageNum == 0, toastError = false)
    }

    /**
     * 今日已祝福次数
     */
    fun loadStatus() {
        call({
            NetManager.getApi().getPrayingStatus()
        }, {
            statusResult.value = successResult(it)
        }, {
            statusResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 发布祈福。coverType>0 为付费封面，服务端扣5功德币
     */
    fun postPraying(content: String, coverType: Int) {
        call({
            NetManager.getApi().postPraying(content = content, coverType = coverType)
        }, {
            postResult.value = successResult(it)
        }, {
            postResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 祝福福卡
     */
    fun blessPraying(prayingId: Int) {
        call({
            NetManager.getApi().blessPraying(prayingId = prayingId)
        }, {
            blessResult.value = successResult(it)
        }, {
            blessResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 删除我的祈福
     */
    fun deletePraying(prayingId: Int) {
        call({
            NetManager.getApi().deletePraying(prayingId = prayingId)
        }, {
            deleteResult.value = successResult(it)
        }, {
            deleteResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }
}

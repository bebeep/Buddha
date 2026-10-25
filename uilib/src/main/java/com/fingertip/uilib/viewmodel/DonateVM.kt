package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.DonateBuddhaBean
import com.fingertip.baselib.bean.DonateRecordBean
import com.fingertip.baselib.bean.LuckyBagBean
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 功德捐赠。对应 donate 一组接口：佛像列表/捐赠/福袋/记录
 */
class DonateVM : TopVMImp() {

    val buddhaListResult = MutableLiveData<RequestResult<List<DonateBuddhaBean>>>()
    val donateResult = MutableLiveData<RequestResult<com.fingertip.baselib.bean.PersonData>>()
    val luckyBagResult = MutableLiveData<RequestResult<LuckyBagBean>>()
    val recordResult = MutableLiveData<RequestResult<List<DonateRecordBean>>>()

    /**
     * 功德佛像列表
     */
    fun loadBuddhaList() {
        call({
            NetManager.getApi().getDonateBuddhaList()
        }, {
            buddhaListResult.value = successResult(it)
        }, {
            buddhaListResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 功德捐赠，服务端返回最新账户信息
     */
    fun donateBuddha(buddhaId: Int, coins: Int) {
        call({
            NetManager.getApi().donateBuddha(buddhaId = buddhaId, coins = coins)
        }, {
            donateResult.value = successResult(it)
        }, {
            donateResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 抢福袋
     */
    fun gainLuckyBag(buddhaId: Int) {
        call({
            NetManager.getApi().gainLuckyBag(buddhaId = buddhaId)
        }, {
            luckyBagResult.value = successResult(it)
        }, {
            luckyBagResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 捐赠记录，buddhaId 传0为全部，pageNum 从 0 开始
     */
    fun loadRecords(pageNum: Int, buddhaId: Int = 0) {
        call({
            NetManager.getApi().getDonateRecord(buddhaId = buddhaId, pageNum = pageNum)
        }, {
            recordResult.value = successResult(it)
        }, {
            recordResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = pageNum == 0, toastError = false)
    }
}

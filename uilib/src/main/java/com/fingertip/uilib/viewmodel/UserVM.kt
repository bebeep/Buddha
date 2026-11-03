package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.MyDressData
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.bean.PracticeStatisticData
import com.fingertip.baselib.bean.UserBlockData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 用户信息。对应 account 的 getUserInfo/modifyProfile/follow/unFollow
 */
class UserVM : TopVMImp() {

    val userInfoResult = MutableLiveData<RequestResult<PersonData>>()
    val modifyResult = MutableLiveData<RequestResult<String>>()
    val followResult = MutableLiveData<RequestResult<String>>()
    val unFollowResult = MutableLiveData<RequestResult<String>>()

    //region 我的页三格数据 + 装扮橱窗（§12.6）

    val practiceStatsResult = MutableLiveData<RequestResult<PracticeStatisticData>>()
    val meditationTotalResult = MutableLiveData<RequestResult<Int>>()
    val myDressResult = MutableLiveData<RequestResult<MyDressData>>()

    /** 早课连续统计：连续修持天数 / 累计诵经次数 */
    fun loadPracticeStats() {
        call({
            NetManager.getApi().getPracticeStatistic()
        }, {
            practiceStatsResult.value = successResult(it)
        }, {
            practiceStatsResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /** 禅值：念佛/禅修累计总数 */
    fun loadMeditationTotal() {
        call({
            NetManager.getApi().getMeditationTotal()
        }, {
            meditationTotalResult.value = successResult(it)
        }, {
            meditationTotalResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /** 装扮橱窗：持有列表 + 佩戴状态 */
    fun loadMyDress() {
        call({
            NetManager.getApi().getMyDress()
        }, {
            myDressResult.value = successResult(it)
        }, {
            myDressResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    //endregion

    /**
     * 获取用户信息
     */
    fun loadUserInfo(accountId: Int) {
        call({
            NetManager.getApi().getUserInfo(accountId = accountId)
        }, {
            userInfoResult.value = successResult(it)
        }, {
            userInfoResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 修改个人资料，传 null 表示不修改该项
     */
    fun modifyProfile(userName: String?, avatar: String?, mood: String?, gender: Int, cover: String?) {
        call({
            NetManager.getApi().modifyProfile(
                userName = userName, avatar = avatar, mood = mood,
                gender = gender, cover = cover
            )
        }, {
            modifyResult.value = successResult(it)
        }, {
            modifyResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 关注
     */
    fun follow(targetAccountId: Int) {
        call({
            NetManager.getApi().follow(targetAccountId = targetAccountId)
        }, {
            followResult.value = successResult(it)
        }, {
            followResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 取消关注
     */
    fun unFollow(targetAccountId: Int) {
        call({
            NetManager.getApi().unFollow(targetAccountId = targetAccountId)
        }, {
            unFollowResult.value = successResult(it)
        }, {
            unFollowResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    //region 拉黑（UGC 合规：与举报并列，是审核必查项）

    val blockResult = MutableLiveData<RequestResult<String>>()
    val unblockResult = MutableLiveData<RequestResult<String>>()
    val blockListResult = MutableLiveData<RequestResult<List<UserBlockData>>>()

    /**
     * 拉黑某人。后端幂等（重复拉黑不报错），这里不做本地判断
     */
    fun addBlock(targetAccountId: Int) {
        call({
            NetManager.getApi().addBlock(targetAccountId = targetAccountId)
        }, {
            blockResult.value = successResult(it)
        }, {
            blockResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 取消拉黑
     */
    fun cancelBlock(targetAccountId: Int) {
        call({
            NetManager.getApi().cancelBlock(targetAccountId = targetAccountId)
        }, {
            unblockResult.value = successResult(it)
        }, {
            unblockResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 拉黑列表。「设置 - 已拉黑用户」用，他人主页也用它判断当前是否已拉黑
     */
    fun loadBlockList() {
        call({
            NetManager.getApi().getBlockList()
        }, {
            blockListResult.value = successResult(it)
        }, {
            blockListResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    //endregion

    //region 举报（针对用户维度，他人主页入口）

    val reportResult = MutableLiveData<RequestResult<String>>()

    /**
     * 举报某个用户
     * @param targetAccountId 被举报人
     * @param reportType 1动态 2评论 3用户
     * @param tag 举报原因，见 ReportDialog.TAG_*
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

    //endregion
}

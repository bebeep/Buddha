package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.SignInData
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp


/**
 * 签到。弹窗打开先拉一次签到信息，签到成功后再拉一次，
 * 本地顺手把金币加上，省得「我的」页面还显示签到前的余额
 */
class SignInVM : TopVMImp() {

    companion object {
        //奖励类型，跟服务端 sign_in_reward_config.rewardType 对齐
        private const val REWARD_TYPE_COIN = 1
    }

    val signInDataResult = MutableLiveData<RequestResult<SignInData>>()
    val signInResult = MutableLiveData<RequestResult<String>>()

    //最近一次拉到的签到信息，签到成功算本次发了多少金币要用
    private var latestData: SignInData? = null

    /**
     * 签到信息：基础奖励、下一档连签奖励、当前连签天数
     */
    fun loadSignInData() {
        call({
            NetManager.getApi().getSignInData()
        }, {
            latestData = it
            signInDataResult.value = successResult(it)
        }, {
            signInDataResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    /**
     * 签到
     */
    fun signIn() {
        call({
            NetManager.getApi().signIn()
        }, {
            addRewardToLocal()
            signInResult.value = successResult(it)
        }, {
            signInResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    /**
     * 服务端签完只回成功失败，没回余额，这里按已知配置把金币补上。
     * 基础奖励每次都有；连签天数刚好走到档位时再加档位奖励
     */
    private fun addRewardToLocal() {
        val data = latestData ?: return
        if (data.hasSignIn) return
        val user = GlobalConfig.userData ?: return

        var coin = data.basicRewardValue(REWARD_TYPE_COIN)
        val next = data.nextContinuousReward
        if (next != null && data.continuousDays + 1 == next.continuousDays) {
            next.rewards?.forEach { if (it.rewardType == REWARD_TYPE_COIN) coin += it.rewardValue }
        }
        if (coin <= 0) return

        user.coin += coin
        GlobalConfig.userData = user
    }
}

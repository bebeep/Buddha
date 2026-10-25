package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.net.NetManager
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
}

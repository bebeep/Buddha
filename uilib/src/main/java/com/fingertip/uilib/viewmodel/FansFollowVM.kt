package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.FollowUserEntity
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 粉丝 / 关注列表。
 *
 * 两个 tab 各用一份 LiveData：即使未来 VM 作用域改成页面级共享，
 * 粉丝页也不会收到关注页的响应，避免分页状态串页。
 */
class FansFollowVM : TopVMImp() {

    val fansResult = MutableLiveData<RequestResult<List<FollowUserEntity>>>()
    val followingResult = MutableLiveData<RequestResult<List<FollowUserEntity>>>()

    /**
     * 粉丝：别人关注我，只查当前登录人
     * @param pageNum 从 0 起，每页 20 条
     */
    fun loadFans(pageNum: Int = 0) {
        call({
            NetManager.getApi().getFans(pageNum = pageNum)
        }, {
            fansResult.value = successResult(it)
        }, {
            fansResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 关注：我关注别人，只查当前登录人
     * @param pageNum 从 0 起，每页 20 条
     */
    fun loadFollowing(pageNum: Int = 0) {
        call({
            NetManager.getApi().getFollowing(pageNum = pageNum)
        }, {
            followingResult.value = successResult(it)
        }, {
            followingResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

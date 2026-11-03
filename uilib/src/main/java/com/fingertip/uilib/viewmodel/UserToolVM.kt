package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.CollectData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 用户工具：收藏（后续佛历提醒也归这里）
 *
 * 单独开一个 VM 而不是塞进 UserVM，是因为收藏有三类目标、要分页、要按类型筛选，
 * 和"看某个人资料"完全是两套数据。
 */
class UserToolVM : TopVMImp() {

    val collectListResult = MutableLiveData<RequestResult<List<CollectData>>>()
    val cancelCollectResult = MutableLiveData<RequestResult<String>>()
    val addCollectResult = MutableLiveData<RequestResult<String>>()
    val isCollectedResult = MutableLiveData<RequestResult<Boolean>>()

    /**
     * 收藏列表
     * @param targetType 1佛经 2感悟 3寺庙，0查全部
     */
    fun loadCollectList(targetType: Int, pageNum: Int = 0) {
        call({
            NetManager.getApi().getCollectList(targetType = targetType, pageNum = pageNum)
        }, {
            collectListResult.value = successResult(it)
        }, {
            collectListResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 取消收藏
     */
    fun cancelCollect(targetType: Int, targetId: Int) {
        call({
            NetManager.getApi().cancelCollect(body = RequestBodyFactory.collectBody(targetType, targetId))
        }, {
            cancelCollectResult.value = successResult(it)
        }, {
            cancelCollectResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 添加收藏
     */
    fun addCollect(targetType: Int, targetId: Int) {
        call({
            NetManager.getApi().addCollect(body = RequestBodyFactory.collectBody(targetType, targetId))
        }, {
            addCollectResult.value = successResult(it)
        }, {
            addCollectResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 是否已收藏，用于详情页初始化按钮状态
     */
    fun isCollected(targetType: Int, targetId: Int) {
        call({
            NetManager.getApi().isCollected(targetType = targetType, targetId = targetId)
        }, {
            isCollectedResult.value = successResult(it)
        }, {
            isCollectedResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

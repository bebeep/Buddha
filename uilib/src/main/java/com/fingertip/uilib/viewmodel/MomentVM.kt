package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.log
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

class MomentVM:TopVMImp() {

    val postMomentResult = MutableLiveData<RequestResult<String>>()
    val momentListResult = MutableLiveData<RequestResult<List<MomentEntity>>>()

    fun postMoment(textContent: String = "",
                   videoUrl: String = "",
                   videoCover: String = "",
                   imageUrl: String = "",
                   videoDuration: Int = 0,
                   videoWidth: Int = 0,
                   videoHeight: Int = 0,
                   isAnonymous: Boolean = false,
                   location: String = "",
                   remindAccountIds: List<Int> = emptyList()) {
        call({
            NetManager.getApi().postMoment(request = RequestBodyFactory.postMomentBody(textContent,
                videoUrl,videoCover,imageUrl,videoDuration,videoWidth,videoHeight,
                isAnonymous,location,remindAccountIds))
        }, {
            postMomentResult.value = successResult(it)
        }, {
            postMomentResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

    fun getMomentList(pageCount: Int,momentType: Int) {
        call({
            when(momentType){
                1 -> NetManager.getApi().getMomentListHot(pageCount = pageCount)
                else -> NetManager.getApi().getMomentListFollowed(pageCount = pageCount)
            }
        }, {
            momentListResult.value = successResult(it)
        }, {
            momentListResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = true)
    }

}
package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.viewmodel.TopVMImp

class MeFragmentVM: TopVMImp() {

    var setAvatarResult = MutableLiveData<String?>()

    fun upAvatar(avatarPath:String){
        call({
            NetManager.getApi().upAvatar(imageUrl=avatarPath)
        }, {
            setAvatarResult.value=avatarPath
        }, {
        }, showLoading = true,toastError = true)
    }
}
package com.fingertip.baselib.top

import com.fingertip.baselib.log
import com.fingertip.baselib.util.PicUtils
import com.fingertip.baselib.view.ChoosePicDialog
import com.fingertip.baselib.viewmodel.TopViewModel
import com.zxy.tiny.callback.FileCallback

/**
 * 带权限申请的fragment
 */
abstract class TopPmFragment<VM : TopViewModel> : TopVMFragment<VM>(),FileCallback {

    val picUtils: PicUtils by lazy { PicUtils() }

    protected val picChoseDialog: ChoosePicDialog by lazy {  ChoosePicDialog(_mActivity,onItemClick={


    }) }



    override fun callback(isSuccess: Boolean, outfile: String?, t: Throwable?) {
        log(fName,"压缩回调--isSc==$isSuccess+outfile==$outfile")
    }



}
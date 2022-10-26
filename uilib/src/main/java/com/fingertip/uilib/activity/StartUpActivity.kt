package com.fingertip.uilib.activity

import com.fingertip.baselib.top.TopVMActivity
import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.StartUpActivityVM

class StartUpActivity : TopVMActivity<StartUpActivityVM>() {

    override fun initVM(): StartUpActivityVM = provideVM()

    override fun layoutId() = R.layout.activity_startup

    override fun isFullTopBar() = true

    override fun initShiTu() {

    }


}
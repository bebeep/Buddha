package com.fingertip.uilib.activity

import com.fingertip.baselib.top.TopActivity
import com.fingertip.uilib.R
import com.fingertip.uilib.fragment.MainFragment

/**
 * 主activity
 */
class ContainerActivity : TopActivity() {


    override fun layoutId() = R.layout.activity_container

    override fun isFullTopBar() = true

    override fun getMainContainerId() = R.id.root_container

    override fun initShiTu() {

        loadRootFragment(mainContainerId, MainFragment())
    }








}
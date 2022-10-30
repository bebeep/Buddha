package com.fingertip.uilib.activity

import com.fingertip.baselib.top.TopActivity
import com.fingertip.uilib.R
import com.fingertip.uilib.fragment.MainFragment

/**
 * 唯一的activity
 *
 * 1、检查服务器状态 - 如果是强更，直接根据返回的地址前往下载
 * 2、自动登录 （除登录信息外 还返回配置信息：包括启动页图、banner图、Tabbar图、各种文案说明等等）
 */
class ContainerActivity : TopActivity() {


    override fun layoutId() = R.layout.activity_container

    override fun isFullTopBar() = true

    override fun getMainContainerId() = R.id.root_container

    override fun initShiTu() {

        loadRootFragment(mainContainerId, MainFragment())
    }








}
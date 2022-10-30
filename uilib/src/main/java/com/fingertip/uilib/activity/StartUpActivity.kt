package com.fingertip.uilib.activity

import android.content.Intent
import com.blankj.utilcode.util.NetworkUtils
import com.fingertip.baselib.dialog.RemindAllDialog
import com.fingertip.baselib.top.TopVMActivity
import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.StartUpVM
import kotlinx.android.synthetic.main.activity_startup.*

/**
 * 1、检查服务器状态 - 如果是强更，直接根据返回的地址前往下载
 * 2、自动登录 （除登录信息外 还返回配置信息：包括启动页图、banner图、Tabbar图、各种文案说明等等）
 */
class StartUpActivity : TopVMActivity<StartUpVM>() {

    override fun initVM(): StartUpVM = provideVM()

    override fun layoutId() = R.layout.activity_startup

    override fun isFullTopBar() = true

    override fun initShiTu() {
        if (!isTaskRoot) {
            val intent = intent
            val action = intent.action
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == action) {
                finish()
                return
            }
        }


        if (!NetworkUtils.isConnected()){
            showBadNetWork()
            return
        }

//        mViewModel.checkServerStatus()

        iv_banner.postDelayed({
            startActivity(Intent(this,ContainerActivity::class.java))
            finish()
        },2000)
    }


    override fun initObserver() {
        mViewModel.serverStatusResult.observe(this) {
            if (!it.success) {//维护

                return@observe
            }

            when(it.data?.status){
                2->{//强更

                }
                3->{//维护

                }
            }
        }
        mViewModel.loginResult.observe(this) {
            if (it.success && it.data != null) {//跳转到主页

            } else { //跳转到登录注册

            }
        }
    }






    private var badNetDialog: RemindAllDialog? = null
    private fun showBadNetWork() {
        loadEnding()
        if (badNetDialog == null) {
            badNetDialog = RemindAllDialog(
                this,
                endAction = {
                    finish()
                },
                tvContent = "网络连接错误",
                textYes = "退出"
            )
        }
        if (badNetDialog?.isShowing == true) {
            return
        }
        badNetDialog?.show()
    }


}
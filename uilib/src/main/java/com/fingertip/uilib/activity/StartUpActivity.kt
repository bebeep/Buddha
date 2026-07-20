package com.fingertip.uilib.activity

import android.content.Intent
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.dialog.RemindAllDialog
import com.fingertip.baselib.top.TopVMActivity
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.databinding.ActivityStartupBinding
import com.fingertip.uilib.viewmodel.StartUpVM
import com.lzlz.toplib.extention.visible

/**
 * 1、检查服务器状态 - 如果是强更，直接根据返回的地址前往下载
 * 2、自动登录 （除登录信息外 还返回配置信息：包括启动页图、banner图、Tabbar图、各种文案说明等等）
 */
class StartUpActivity : TopVMActivity<StartUpVM>() {

    override fun initVM(): StartUpVM = provideVM()

    lateinit var binding: ActivityStartupBinding


    override fun isFullTopBar() = true

    override fun initShiTu() {
        binding = ActivityStartupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!isTaskRoot) {
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == intent.action) {
                finish()
                return
            }
        }


        if (!NetworkUtils.isConnected()){
            showBadNetWork()
            return
        }

        mViewModel.checkServerStatus()

        binding.ivBanner.isEnabled = false
        binding.ivBanner.setOnClickListener {
            startActivity(Intent(this,ContainerActivity::class.java))
            finish()
        }
    }


    override fun initObserver() {
        mViewModel.serverStatusResult.observe(this) {
            if (!it.success) {//接口请求失败
                showBadNetWork()
                return@observe
            }
            GlobalConfig.versionInfo = it.data
            when(it.data?.versionStatus){
                1->{//正常
                    GlobalConfig.globalParam = it.data?.globalParam
                    val username = SPUtils.getInstance(SPConstant.SP_ACCOUNT).getString(SPConstant.SP_ACCOUNT_USERNAME)
                    val password = SPUtils.getInstance(SPConstant.SP_ACCOUNT).getString(SPConstant.SP_ACCOUNT_PASSWORD)
                    if (username.isNotEmpty() && password.isNotEmpty()){//情况正常，并且本地保存了账号密码 直接登录
                        mViewModel.login(username,password)
                    }
                    else
                    {
                        startActivity(Intent(this, LoginRegisterActivity::class.java))
                        finish()
                    }
                }
                2->{//维护
                    RemindAllDialog(
                        this,
                        endAction = {
                            finish()
                        },
                        tvContent = "维护中，请稍后再试",
                        textYes = "退出"
                    ).show()
                }
                3->{//强更
                    RemindAllDialog(
                        this,
                        endAction = {
                            finish()
                        },
                        tvContent = "强更",
                        textYes = "立即更像",
                        textNo = "退出"
                    ).show()
                }
                4->{//软更
                    ToastUtil.showMessage("有新版本可用，请更新")
                }
            }
        }
        mViewModel.loginResult.observe(this) {
            if (it.success && it.data != null) {//跳转到主页
                binding.ivBanner.visible()
                binding.ivBanner.isEnabled = true
                binding.ivBanner.postDelayed({
                    startActivity(Intent(this,ContainerActivity::class.java))
                    finish()
                },2000)
            } else { //跳转到登录注册
                startActivity(Intent(this, LoginRegisterActivity::class.java))
                finish()
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
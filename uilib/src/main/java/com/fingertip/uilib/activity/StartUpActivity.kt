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
            goHome()
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
                    // 冷启动自动登录：只用 session 续期。
                    // 旧版存的是「手机号+明文密码」，升级上来的机器没有 session，直接去登录页，
                    // 同时把旧的明文凭证清掉。
                    val sp = SPUtils.getInstance(SPConstant.SP_ACCOUNT)
                    val session = sp.getString(SPConstant.SP_SESSION)
                    if (session.isNotEmpty()) {
                        mViewModel.refreshSession(session)
                    } else {
                        sp.remove(SPConstant.SP_ACCOUNT_PASSWORD)
                        sp.remove(SPConstant.SP_ACCOUNT_USERNAME)
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
            val data = it.data
            if (it.success && data != null) {//跳转到主页
                GlobalConfig.userData = data
                // 必须在 GlobalConfig.userData 赋值之后再上报：设备档案要用到 session
                com.fingertip.uilib.push.DeviceProfile.report()
                // IM 登录：换设备后新设备登录即自动云端同步会话与漫游消息
                com.fingertip.uilib.im.ImManager.init(this)
                com.fingertip.uilib.im.ImManager.login(data.accountId)
                binding.ivBanner.visible()
                binding.ivBanner.isEnabled = true
                val askedPermission = com.fingertip.uilib.push.DeviceProfile.requestNotificationPermission(this)
                if (askedPermission) {
                    //首次弹通知权限：等用户在弹窗上操作完再进首页，别让倒计时把首页跳出来压在弹窗下面
                    pendingJumpHome = true
                } else {
                    binding.ivBanner.postDelayed({ goHome() }, 2000)
                }
            } else { //跳转到登录注册
                startActivity(Intent(this, LoginRegisterActivity::class.java))
                finish()
            }
        }
    }

    /** 权限弹窗挂起期间的跳首页标记 */
    private var pendingJumpHome = false

    private fun goHome() {
        pendingJumpHome = false
        startActivity(Intent(this, ContainerActivity::class.java))
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == com.fingertip.uilib.push.DeviceProfile.REQ_NOTIFICATION && pendingJumpHome) {
            //用户已在权限弹窗上操作（允许/拒绝都算），稍等弹窗收起再进首页
            binding.ivBanner.postDelayed({ if (!isFinishing && !isDestroyed) goHome() }, 300)
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
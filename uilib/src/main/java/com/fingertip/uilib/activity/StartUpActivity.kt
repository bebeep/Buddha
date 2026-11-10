package com.fingertip.uilib.activity

import android.content.Intent
import android.util.Log
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.SPUtils
import com.bumptech.glide.Glide
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.dialog.RemindAllDialog
import com.fingertip.baselib.top.TopVMActivity
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.databinding.ActivityStartupBinding
import com.fingertip.uilib.util.SplashImageCache
import com.fingertip.uilib.viewmodel.StartUpVM
import com.lzlz.toplib.extention.visible

/**
 * 1、检查服务器状态 - 如果是强更，直接根据返回的地址前往下载
 * 2、自动登录 （除登录信息外 还返回配置信息：包括启动页图、banner图、Tabbar图、各种文案说明等等）
 */
class StartUpActivity : TopVMActivity<StartUpVM>() {

    override fun initVM(): StartUpVM = provideVM()

    lateinit var binding: ActivityStartupBinding

    /** 屏幕分辨率，供启动图按设备缩放解码，避免大图卡顿 */
    private var screenW = 0
    private var screenH = 0

    /** 本地缓存启动图是否已解码并准备显示（用于决定是否延长启动页停留，避免被跳转抢走） */
    private var hasSplashCache = false

    /** 服务端已决定跳转、但需等本地缓存图显示完再执行的动作 */
    private var pendingJump: (() -> Unit)? = null


    override fun isFullTopBar() = true

    override fun initShiTu() {
        binding = ActivityStartupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        screenW = resources.displayMetrics.widthPixels
        screenH = resources.displayMetrics.heightPixels
        hasSplashCache = false
        pendingJump = null

        if (!isTaskRoot) {
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == intent.action) {
                finish()
                return
            }
        }

        // 进启动页先用本地缓存的启动图盖满（断网也能显示，不依赖网络），避免 logo 闪烁/空白。
        // 首帧仍由编译期 windowBackground(bg_startup) 兜底，这里盖住第二帧及之后。
        showCachedSplash()

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
            // 启动页图：后台静默下载到本地缓存，下次启动直接显示（不阻塞本次启动）
            it.data?.globalParam?.splashUrl?.takeIf { u -> u.isNotEmpty() }?.let { url ->
                if (url != SplashImageCache.currentUrl()
                    || SplashImageCache.isExpired()
                    || SplashImageCache.getCacheFile(this) == null) {
                    downloadSplashAsync(url)
                }
            }
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
                        val jump = {
                            startActivity(Intent(this, LoginRegisterActivity::class.java))
                            finish()
                        }
                        if (hasSplashCache) {
                            // 本地缓存图已显示，停留一会儿让用户看到再跳登录页
                            binding.ivBanner.postDelayed(jump, 1200)
                        } else {
                            // 本地缓存图尚未解码完：挂起跳转，等解码结果再决定
                            // （有缓存则延迟显示，无缓存则立即跳，保持原快速过场）
                            pendingJump = jump
                            // 异常保底：2.5s 后无论如何跳转，避免卡死
                            binding.ivBanner.postDelayed({
                                if (pendingJump != null) {
                                    pendingJump?.invoke()
                                    pendingJump = null
                                }
                            }, 2500)
                        }
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

    /**
     * 进启动页先用本地缓存图盖满 ivBanner（全屏 centerCrop），盖住 windowBackground。
     * 缓存不存在则跳过（保持 bg_startup 兜底，不空白）。子线程解码，避免大图卡主线程。
     */
    private fun showCachedSplash() {
        val file = SplashImageCache.getCacheFile(this)
        if (file == null) {
            // 无本地缓存：若服务端已决定跳转，立即执行（保持原快速过场，不卡）
            if (pendingJump != null) {
                pendingJump?.invoke()
                pendingJump = null
            }
            return
        }
        Thread {
            val bmp = SplashImageCache.decodeSampled(file.absolutePath, screenW, screenH)
            runOnUiThread {
                if (bmp != null) {
                    hasSplashCache = true
                    binding.ivBanner.setImageBitmap(bmp)
                    binding.ivBanner.visible()
                    Log.d("SplashCache", "show cached splash, fileLen=${file.length()}")
                    // 服务端已决定跳转：等用户看到缓存图再跳
                    if (pendingJump != null) {
                        binding.ivBanner.postDelayed({ pendingJump?.invoke(); pendingJump = null }, 1200)
                    }
                } else {
                    Log.e("SplashCache", "decode cached splash failed")
                    if (pendingJump != null) {
                        pendingJump?.invoke()
                        pendingJump = null
                    }
                }
            }
        }.start()
    }

    /**
     * 后台用 Glide 下载启动图原图（不阻塞启动），缩放压缩后存到 filesDir 本地缓存，
     * 同时记录当前 url；下次启动 showCachedSplash 直接读本地显示。
     */
    private fun downloadSplashAsync(url: String) {
        Thread {
            try {
                Log.d("SplashCache", "start download: $url")
                val src = Glide.with(applicationContext).download(url).submit().get()
                Log.d("SplashCache", "downloaded file len=${src.length()}")
                val bmp = SplashImageCache.decodeSampled(src.absolutePath, screenW, screenH)
                if (bmp != null) {
                    SplashImageCache.save(this, bmp, 7)
                    SplashImageCache.setCurrentUrl(url)
                    Log.d("SplashCache", "saved splash bitmap ${bmp.width}x${bmp.height}")
                } else {
                    Log.e("SplashCache", "decode downloaded file failed")
                }
            } catch (e: Exception) {
                Log.e("SplashCache", "download failed: ${e.message}")
                e.printStackTrace()
            }
        }.start()
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

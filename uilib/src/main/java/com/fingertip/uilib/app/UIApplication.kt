package com.fingertip.uilib.app

import com.blankj.utilcode.util.LanguageUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.language.LanguageUtil
import com.fingertip.baselib.top.TopApplication
//import com.fingertip.uilib.BuildConfig
import com.fingertip.uilib.R
import com.scwang.smart.refresh.footer.BallPulseFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.tencent.bugly.crashreport.CrashReport
import com.weikaiyun.fragmentation.Fragmentation
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager


/**
 * 主application
 */
class UIApplication: TopApplication() {

    override fun onCreate() {
        super.onCreate()
        PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
        val local = LanguageUtils.getSystemLanguage()//当前系统语言
        val customLan = SPUtils.getInstance().getString("customLanguage")
        LogUtils.e("language===============${local.language}  |  $customLan")
        if (customLan.isNullOrEmpty()) {
            if (LanguageUtil.languages.contains(local.language))SPUtils.getInstance().put("customLanguage",local.language)
            else SPUtils.getInstance().put("customLanguage",LanguageUtil.languages[0])
        }

        LogUtils.e("language===============${LanguageUtils.getAppContextLanguage().language}")
        CrashReport.initCrashReport(applicationContext, "111", false)
        initRefresh()
        initFragment()
        // IM SDK 初始化：不依赖登录态，login 在登录成功回调里做
        com.fingertip.uilib.im.ImManager.init(this)
        // 前后台状态同步给 IM 后台：退后台走厂商通道离线推送，回前台恢复长连接
        registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
            private var started = 0
            override fun onActivityStarted(activity: android.app.Activity) {
                if (started == 0) com.fingertip.uilib.im.ImPushHelper.goForeground()
                started++
            }

            override fun onActivityStopped(activity: android.app.Activity) {
                started--
                if (started == 0) com.fingertip.uilib.im.ImPushHelper.goBackground()
            }

            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
        // Firebase 初始化放最后：它内部有 try-catch，任何异常都不该影响启动
        com.fingertip.uilib.push.DeviceProfile.initFirebase(this)
        // Billing 连接放到启动阶段异步建好，等用户点购买时就不用再等握手
        com.fingertip.uilib.pay.BillingManager.init(this)
    }


    /**
     * 下拉刷新控件的多语言
     */
    private fun initRefresh(){
        //设置全局的Header构建器
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
//            layout.setPrimaryColorsId(R.color.colorPrimary, android.R.color.white) //全局设置主题颜色
            MaterialHeader(context) //.setTimeFormat(new DynamicTimeFormat("更新于 %s"));//指定为经典Header，默认是 贝塞尔雷达Header
        }
        //设置全局的Footer构建器
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, layout -> //指定为经典Footer，默认是 BallPulseFooter
            BallPulseFooter(context)
        }
    }


    private fun initFragment(){
        Fragmentation.builder() // 设置 栈视图 模式为 （默认）悬浮球模式   SHAKE: 摇一摇唤出  NONE：隐藏， 仅在Debug环境生效
            .stackViewMode(Fragmentation.BUBBLE)
            .debug(false) // 实际场景建议.debug(BuildConfig.DEBUG)
            .animation(R.anim.h_fragment_enter, R.anim.h_fragment_pop_exit, R.anim.h_fragment_pop_enter, R.anim.h_fragment_exit) //设置默认动画
            .install()
    }
}
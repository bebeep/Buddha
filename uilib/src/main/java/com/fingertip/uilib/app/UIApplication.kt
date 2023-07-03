package com.fingertip.uilib.app

import com.blankj.utilcode.util.LanguageUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.language.LanguageUtil
import com.fingertip.baselib.top.TopApplication
import com.fingertip.uilib.BuildConfig
import com.fingertip.uilib.R
import com.scwang.smart.refresh.footer.BallPulseFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.tencent.bugly.crashreport.CrashReport
import com.weikaiyun.fragmentation.Fragmentation


/**
 * 主application
 */
class UIApplication: TopApplication() {

    override fun onCreate() {
        super.onCreate()
        val local = LanguageUtils.getSystemLanguage()//当前系统语言
        val customLan = SPUtils.getInstance().getString("customLanguage")
        LogUtils.e("language===============${local.language}  |  $customLan")
        if (customLan.isNullOrEmpty()) {
            if (LanguageUtil.languages.contains(local.language))SPUtils.getInstance().put("customLanguage",local.language)
            else SPUtils.getInstance().put("customLanguage",LanguageUtil.languages[0])
        }

        LogUtils.e("language===============${LanguageUtils.getAppContextLanguage().language}")
        CrashReport.initCrashReport(applicationContext, "111", BuildConfig.DEBUG)
        initRefresh()
        initFragment()
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
            .debug(BuildConfig.DEBUG) // 实际场景建议.debug(BuildConfig.DEBUG)
            .animation(R.anim.h_fragment_enter, R.anim.h_fragment_pop_exit, R.anim.h_fragment_pop_enter, R.anim.h_fragment_exit) //设置默认动画
            .install()
    }
}
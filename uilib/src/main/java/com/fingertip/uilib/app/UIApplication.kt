package com.fingertip.uilib.app

import com.blankj.utilcode.util.LanguageUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.language.LanguageUtil
import com.fingertip.baselib.top.TopApplication
import com.fingertip.uilib.BuildConfig
import com.scwang.smart.refresh.footer.BallPulseFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.tencent.bugly.crashreport.CrashReport
import io.github.devzwy.nsfw.NSFWHelper


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

        if (BuildConfig.DEBUG){
            NSFWHelper.openDebugLog()
        }
        NSFWHelper.initHelper(context = this,isOpenGPU = false)
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
}
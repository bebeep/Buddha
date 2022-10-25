package com.fingertip.uilib.app
import com.fingertip.baselib.top.TopApplication
import com.scwang.smart.refresh.footer.BallPulseFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

open class AppApplication: TopApplication() {

    private val TAG = "AppApplication"

    override fun onCreate() {
        super.onCreate()
        initSmartRefreshHeaderFooter()
    }

    private fun initSmartRefreshHeaderFooter() {
        //设置全局的Header构建器
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
//            layout.setPrimaryColorsId(R.color.colorPrimary, android.R.color.white) //全局设置主题颜色
            ClassicsHeader(context) //.setTimeFormat(new DynamicTimeFormat("更新于 %s"));//指定为经典Header，默认是 贝塞尔雷达Header
        }
        //设置全局的Footer构建器
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, layout -> //指定为经典Footer，默认是 BallPulseFooter
            BallPulseFooter(context)
        }
    }

}
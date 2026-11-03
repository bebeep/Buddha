package com.fingertip.baselib.top

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.fingertip.baselib.event_bus.EventBusProxy
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.util.FullTopUtil
import com.fingertip.baselib.view.LoadingDialog
import com.weikaiyun.fragmentation.SupportActivity
import org.greenrobot.eventbus.Subscribe

abstract class TopActivity : SupportActivity(), View.OnClickListener {
    lateinit var activity: TopActivity
    protected var loadDialog: LoadingDialog? = null
    var aTAG: String = this::class.java.simpleName


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        EventBusProxy.register(this)
        activity = this
        FullTopUtil.setStatusTranslucent(this)
        FullTopUtil.FitsSystemWindows(this, !isFullTopBar())
        //海外版全局暗底：状态栏字体/图标用白色（dark=false 即不亮色状态栏=白字）
        if (!FullTopUtil.setStatusBarDarkTheme(this, false)) {
            //不支持设置状态栏字体风格时，兜底给状态栏垫半透明黑，保证白字可读
            FullTopUtil.setWindowStatusBarColor(this, 0x55000000);
        }
        initShiTu()
        addViewClickEvent()
    }



    /**
     * 是否让布局充满statusBar
     */
    protected abstract fun isFullTopBar():Boolean

    /**
     * 布局文件初始化
     */
    protected abstract fun initShiTu()



    fun loading(cancelable: Boolean ?=false) {
        if (loadDialog == null) {
            loadDialog = LoadingDialog(this)
        }
        loadDialog?.run {
            setCancelable(cancelable?:false)
            if (!isShowing) {
                show()
            }
        }
    }
    fun loadEnding() {
        log("endLoading.....","")
        loadDialog?.apply {
            if (isShowing)
                dismiss()
        }
        loadDialog = null
    }

    override fun onDestroy() {
        EventBusProxy.unRegister(this)
        super.onDestroy()
        loadEnding()
    }

    open fun listOfClickView(): List<View> = listOf()

    private fun addViewClickEvent() {
        TopUtils.restore()
        for (view in listOfClickView()) {
            view.setOnClickListener(this)
        }
    }

    override fun onClick(v: View?) {
        if (!TopUtils.isTwiceClick()) {
            respondOnceClick(v)
        }
    }

    open fun respondOnceClick(v: View?){

    }

    @Subscribe
    fun onReceiveMessage(event: MessageEvent){

    }
}
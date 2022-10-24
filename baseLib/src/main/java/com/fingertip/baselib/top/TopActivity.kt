package com.fingertip.baselib.top

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.fingertip.baselib.log
import com.fingertip.baselib.util.FullTopUtil
import com.fingertip.baselib.view.LoadingDialog
import me.yokeyword.fragmentation.SupportActivity

abstract class TopActivity : SupportActivity(), View.OnClickListener{
    lateinit var activity: TopActivity
    protected var loadDialog: LoadingDialog? = null
    var aTAG: String =this::class.java.simpleName


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        activity=this
        setContentView(layoutId())
        FullTopUtil.setStatusTranslucent(this)
        FullTopUtil.FitsSystemWindows(this, !isFullTopBar())
        if (!FullTopUtil.setStatusBarDarkTheme(this, true)) {
            //不支持设置深色风格 总不能让状态栏都是白色,设置一个状态栏颜色为半透明,
            //半透明+白=灰
            FullTopUtil.setWindowStatusBarColor(this, 0x55000000);
        }
        addViewClickEvent()
        initShiTu()
    }

    /**
     * 布局文件id
     */
    protected abstract fun layoutId(): Int


    /**
     * 是否让布局充满statusBar
     */
    protected abstract fun isFullTopBar():Boolean

    /**
     * 布局文件初始化
     */
    protected abstract fun initShiTu()


    fun loading() {
        loading(true)
    }

    fun loading(cancelable: Boolean) {
        log("startLoading.....",cancelable)
        if (loadDialog == null) {
            loadDialog = LoadingDialog(this)
        }
        loadDialog?.run {
            setCancelable(cancelable)
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

}
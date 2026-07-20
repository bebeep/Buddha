package com.fingertip.baselib.top

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.LogUtils
import com.fingertip.baselib.event_bus.EventBusProxy
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.util.registerSoftInputChangedListener
import com.fingertip.baselib.util.unregisterSoftInputChangedListener

import com.fingertip.baselib.view.LoadingDialog
import com.weikaiyun.fragmentation.SupportFragment

import org.greenrobot.eventbus.Subscribe

abstract class TopFragment : SupportFragment(), View.OnClickListener, KeyboardUtils.OnSoftInputChangedListener {

    protected val fName by lazy { javaClass.simpleName }

    protected var mWaitingDialog: LoadingDialog? = null

    var mBinding: ViewBinding? = null
        protected set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = createViewBinding(inflater, container)
        return mBinding?.root ?: View.inflate(context, layoutId(), null)
    }

    private fun createViewBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding? {
        return try {
            val layoutName = resources.getResourceEntryName(layoutId())
            val bindingName = layoutName.split("_")
                .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + "Binding"
            
            // 尝试多个可能的包路径（模块级databinding包、当前包等）
            val packageName = javaClass.`package`?.name ?: ""
            val packageParts = packageName.split(".")
            val possiblePackages = mutableListOf<String>()
            
            // 尝试当前包及其父包的 databinding 子包
            for (i in packageParts.size downTo 2) {
                possiblePackages.add(packageParts.subList(0, i).joinToString(".") + ".databinding")
            }
            // 也尝试当前包
            possiblePackages.add(packageName)
            
            var bindingClass: Class<*>? = null
            for (pkg in possiblePackages) {
                try {
                    bindingClass = Class.forName("$pkg.$bindingName")
                    break
                } catch (e: ClassNotFoundException) {
                    continue
                }
            }
            
            if (bindingClass == null) {
                log(fName, "未找到Binding类: $bindingName")
                return null
            }
            
            val inflateMethod = bindingClass.getMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.javaPrimitiveType
            )
            inflateMethod.invoke(null, inflater, container, false) as? ViewBinding
        } catch (e: Exception) {
            log(fName, "ViewBinding创建失败，回退到普通inflate: ${e.message}")
            null
        }
    }

    /**
     * 布局文件ID
     */
    protected abstract fun layoutId(): Int

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        EventBusProxy.register(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        log(fName, "onActivityCreated")
//        if (!getLazyInit()) {
            build()
//        }
    }


    private fun build() {
        LogUtils.e("================================${view==null}")
        if (view==null){
            return
        }
        buildViewClick()
        initShiTu()
    }

    /**
     * 布局文件初始化
     */
    protected abstract fun initShiTu()

    open fun getLazyInit() = true

    var isFirstResume = false

    override fun onResume() {
        super.onResume()
        log(fName, "onResume--")
        if (!isFirstResume && view != null) {
            isFirstResume = true
            onFirstResume()
        }
    }

    override fun onPause() {
        super.onPause()
        log(fName, "onPause--")
    }

    open fun onFirstResume() {
        log(fName, "onFirstResume--")
    }

    override fun onDestroyView() {
        EventBusProxy.unRegister(this)
        mBinding = null
        super.onDestroyView()
        log(fName, "onDestroyView--")
        loadEnding()
    }

    open fun getClickViews(): List<View> = listOf()

    private fun buildViewClick() {
        TopUtils.restore()
        for (view in getClickViews()) {
            view.setOnClickListener(this)
        }
    }

    override fun onClick(v: View?) {
        if (!TopUtils.isTwiceClick()) {
            onSingleClick(v)
        }
    }

    open fun onSingleClick(v: View?){}

    //****************** 弹窗相关 *******************

    fun startWaiting() {
        loading(true)
    }

    fun loading(cancelable: Boolean) {
        log(value = "startLoading.....")
        activity?.let {
            if (mWaitingDialog == null) {
                mWaitingDialog = LoadingDialog(it)
            }
        }
        mWaitingDialog?.run {
            setCancelable(cancelable)
            if (!isShowing) {
                show()
            }
        }
    }
    fun loadEnding() {
        log(value = "endLoading.....")
        mWaitingDialog?.apply {
            if (isShowing)
                dismiss()
        }
        mWaitingDialog = null
    }


    @Subscribe()
    fun noFun(event: MessageEvent) {
        when(event.what) {
            EventConstant.EVENT_NOTIFY_REGISTER_KEYBOARD -> {
                if (event.tag != javaClass.simpleName) {
                    registerKeyboardIfNeed()
                }
            }

            EventConstant.EVENT_NOTIFY_UNREGISTER_KEYBOARD -> {
                if (event.tag != javaClass.simpleName) {
                    unregisterKeyboardIfNeed()
                }
            }
        }
    }


    // ----------------- 软键盘相关 ---------------

    /**
     * 如果页面需要使用软键盘，重写此方法返回true
     */
    open fun useKeyboard() = false

    /**
     * 如果页面需要使用软键盘，重写此方法，返回标识本页面的唯一id
     */
    open fun getKeyboardTag() = 0

    /**
     * 如果是透明背景需要重写此方法
     */
    open fun isDialogFragment() = false

    override fun onVisible() {
        super.onVisible()
        log(fName, "onSupportVisible--")

        registerKeyboardIfNeed()

        if (useKeyboard() && isDialogFragment()) {
            EventBusProxy.post(MessageEvent(EventConstant.EVENT_NOTIFY_UNREGISTER_KEYBOARD).apply { tag = this@TopFragment.javaClass.simpleName })
        }
    }

    fun registerKeyboardIfNeed() {
        if (useKeyboard()) {
            activity?.window?.let {
                registerSoftInputChangedListener(it,this,getKeyboardTag())
            }
        }
    }

    override fun onInvisible() {
        super.onInvisible()
        log(fName, "onSupportInvisible--")

        unregisterKeyboardIfNeed()

        if (useKeyboard() && isDialogFragment()) {
            EventBusProxy.post(MessageEvent(EventConstant.EVENT_NOTIFY_REGISTER_KEYBOARD).apply { tag = this@TopFragment.javaClass.simpleName })
        }
    }

    fun unregisterKeyboardIfNeed() {
        if (useKeyboard()) {
            activity?.window?.let { unregisterSoftInputChangedListener(it, getKeyboardTag()) }
            onSoftInputChanged(0)
            KeyboardUtils.hideSoftInput(_mActivity)
        }
    }

    override fun onSoftInputChanged(height: Int) {}


}
package com.fingertip.baselib.top

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.fingertip.baselib.R
import com.google.android.material.bottomsheet.BottomSheetDialog


@Suppress("LeakingThis")
abstract class TopDialogBottomSheetDialog(context: Context) : BottomSheetDialog(context, R.style.gd_custom_dialog), DialogInterface.OnDismissListener {

    companion object {
        lateinit var TAG: String
    }

    //onViewCreate 是否已经执行过，避免重复绑定
    private var viewCreated = false

    /**
     * 已经 inflate 好的布局。子类需要提前操作视图时可配合 [ensureViewCreated] 使用
     */
    protected lateinit var contentView: View
        private set

    init {
        TAG = javaClass.simpleName
        val view = View.inflate(context, getLayoutId(), null)
        contentView = view
        setContentView(view)
        window?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)


        behavior.isHideable = false
        setOnDismissListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //必须等子类构造完成再回调：子类的构造参数是在 super 构造之后才赋值的，
        //放在 init 里回调会让 onViewCreate 里读到的构造参数全是 null
        ensureViewCreated()
    }

    /**
     * 触发 [onViewCreate]，只会执行一次。
     * 有子类需要在 show() 之前操作视图的，自己在用之前调一下即可
     */
    protected fun ensureViewCreated() {
        if (viewCreated) return
        viewCreated = true
        onViewCreate(contentView)
    }

    /**
     * 布局文件ID
     */
    abstract fun getLayoutId(): Int

    abstract fun onViewCreate(view: View)

    override fun onDismiss(dialog: DialogInterface?) {
    }
}

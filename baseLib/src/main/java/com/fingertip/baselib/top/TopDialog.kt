package com.fingertip.baselib.top

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.fingertip.baselib.R


abstract class TopDialog @JvmOverloads constructor(context: Context, themeResId: Int = R.style.gd_custom_dialog) : Dialog(context, themeResId), DialogInterface.OnDismissListener {
    companion object {
        lateinit var TAG: String
    }

    init {
        TAG = javaClass.simpleName
    }

    private var mOnDismissListener: DialogInterface.OnDismissListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view: View = View.inflate(context, attachLayoutRes(), null)
        if (IS_MATCH_PARENT()) {
            setContentView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            setContentView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        onViewInit(view)
        super.setOnDismissListener(this)
    }

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        mOnDismissListener = listener
    }

    /**
     * 布局文件ID
     */
    protected abstract fun attachLayoutRes(): Int

    protected abstract fun onViewInit(view: View?)

    open fun IS_MATCH_PARENT():Boolean {
        return false
    }



    override fun onDismiss(dialog: DialogInterface) {
        if (mOnDismissListener != null) {
            mOnDismissListener!!.onDismiss(dialog)
        }
    }


    override fun show() {
        val context = context
        if (context is Activity) {
            val activity = context
            if (activity == null || activity.isDestroyed || activity.isFinishing) return
        }
        super.show()
    }
    override fun dismiss() {
        if (!isShowing) {
            return
        }
        if (context is Activity) {
            val a = context as Activity
            if (a == null || a.isDestroyed || a.isFinishing) {
                return
            }
        }
        super.dismiss()
    }

    fun setBottomDialog() {
        window?.setGravity(Gravity.BOTTOM)
        window?.setWindowAnimations(R.style.gd_dialog_bottom_style)
    }
}
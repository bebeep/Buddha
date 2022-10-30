package com.fingertip.baselib.top

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.fingertip.baseLib.R
import com.google.android.material.bottomsheet.BottomSheetDialog


@Suppress("LeakingThis")
abstract class TopDialogBottomSheetDialog(context: Context) : BottomSheetDialog(context, R.style.gd_custom_dialog), DialogInterface.OnDismissListener {

    companion object {
        lateinit var TAG: String
    }

    init {
        TAG = javaClass.simpleName
        val view = LayoutInflater.from(context).inflate(getLayoutId(), null)
        setContentView(view)
        window?.findViewById<FrameLayout>(R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
        onViewCreate(view)

        setOnDismissListener(this)
    }

    abstract fun getLayoutId(): Int

    abstract fun onViewCreate(view: View)

    override fun onDismiss(dialog: DialogInterface?) {
    }
}
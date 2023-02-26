package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import kotlinx.android.synthetic.main.dialog_sign_in.*

/**
 * 签到
 */
class SignInDialog(context: Context):TopDialog(context) {
    override fun attachLayoutRes(): Int = R.layout.dialog_sign_in

    override fun onViewInit(view: View?) {



        cl_shadow.setOnClickListener {
            ToastUtil.showMessage("签到")
        }

        iv_close.setOnClickListener { dismiss() }
    }


    fun show(content:String){
        show()
        tv_sign_in.text = content
    }
}
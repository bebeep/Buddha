package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.top.TopDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R

/**
 * 签到
 */
class SignInDialog(context: Context):TopDialog(context) {
    override fun attachLayoutRes(): Int = R.layout.dialog_sign_in

    override fun onViewInit(view: View?) {



        view?.findViewById<View>(R.id.cl_shadow)?.setOnClickListener { viewNotNull ->
            ToastUtil.showMessage("签到")
        }

        view?.findViewById<View>(R.id.iv_close)?.setOnClickListener { dismiss() }
    }


    fun show(content:String){
        show()
        findViewById<android.widget.TextView>(R.id.tv_sign_in).text = content
    }
}
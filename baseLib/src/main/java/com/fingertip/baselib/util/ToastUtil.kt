package com.fingertip.baselib.util

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.fingertip.baseLib.R
import com.fingertip.baselib.top.TopApplication
import kotlinx.android.synthetic.main.view_toast.view.*


import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object ToastUtil {

    var toast: Toast? = null
    var tvContent:TextView?=null
    private fun initToast() {
        if (toast == null) {
            toast = Toast(TopApplication.instance)
            toast?.setGravity(Gravity.FILL_HORIZONTAL or Gravity.CENTER, 0, 0)
            val layout: View =
                LayoutInflater.from(TopApplication.instance).inflate(R.layout.view_toast, null)
            tvContent=layout.tv_text
            toast?.view = layout
        }
    }
    var job: Job?=null
    var isShow=false


    fun showMessage(msg: String, time: Long=Toast.LENGTH_SHORT.toLong()) {

        if (isShow){
            toast?.cancel()
            toast=null
            job?.cancel()
            isShow=false
        }
        initToast()
        toast?.run {
            tvContent?.text=msg
            duration=time.toInt()
        }

        job= MainScope().launch {
            delay(time)
            isShow=false
        }


        toast?.show()
        isShow=true
    }
}
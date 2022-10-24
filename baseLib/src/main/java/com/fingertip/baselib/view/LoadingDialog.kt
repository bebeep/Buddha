package com.fingertip.baselib.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.fingertip.baseLib.R
import com.fingertip.baselib.util.loadWeabpGif
import kotlinx.android.synthetic.main.dialog_c.*


class LoadingDialog(c:Context) : Dialog(c, R.style.dialog_load){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var v = LayoutInflater.from(context).inflate(R.layout.dialog_c, null)
        setContentView(
            v,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setCancelable(true)

    }

    override fun show() {
        super.show()
        com.fingertip.baselib.util.loadWeabpGif(R.mipmap.live_loading)
    }

    override fun dismiss() {

        super.dismiss()
        try {
            Glide.with(gif).clear(gif)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
package com.fingertip.baselib.view

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.fingertip.baselib.R
import com.fingertip.baselib.util.loadWebpGif


class LoadingDialog(c:Context) : Dialog(c, R.style.dialog_load){
    private var gifView: android.widget.ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var v = LayoutInflater.from(context).inflate(R.layout.dialog_c, null)
        gifView = v.findViewById(R.id.gif)
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
        gifView?.loadWebpGif(R.mipmap.live_loading)
    }

    override fun dismiss() {

        super.dismiss()
        try {
            val v = gifView ?: return
            Glide.with(v).clear(v)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
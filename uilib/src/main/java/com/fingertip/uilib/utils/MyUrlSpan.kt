package com.fingertip.uilib.utils

import android.text.style.ClickableSpan
import android.view.View
import com.fingertip.baselib.util.FragmentFactory.instanceWebFragment
import com.weikaiyun.fragmentation.SupportActivity

class MyUrlSpan(private val url: String) : ClickableSpan() {
    override fun onClick(view: View) {
        (view.context as SupportActivity).start(
            instanceWebFragment(
                url
            )
        )
    }
}
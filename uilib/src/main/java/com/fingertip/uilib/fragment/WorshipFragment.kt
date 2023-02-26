package com.fingertip.uilib.fragment

import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopFragment
import com.fingertip.uilib.R
import org.greenrobot.eventbus.Subscribe

/**
 * 拜佛
 */
class WorshipFragment : TopFragment() {
    override fun layoutId(): Int = R.layout.fragment_worship

    override fun initShiTu() {


    }





    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when(event.what) {

        }
    }
}
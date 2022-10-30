package com.fingertip.uilib.fragment

import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopFragment
import com.fingertip.uilib.R
import org.greenrobot.eventbus.Subscribe

/**
 * 首页
 */
class HostFragment : TopFragment() {
    override fun layoutId(): Int = R.layout.fragment_host

    override fun initShiTu() {


    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        refreshData()

    }


    private fun refreshData() {
    }



    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when(event.what) {

        }
    }
}
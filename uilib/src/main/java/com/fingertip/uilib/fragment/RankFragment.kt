package com.fingertip.uilib.fragment

import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopFragment
import com.fingertip.uilib.R
import org.greenrobot.eventbus.Subscribe

/**
 * 排行榜
 */
class RankFragment : TopFragment() {
    override fun layoutId(): Int = R.layout.fragment_rank

    override fun initShiTu() {


    }

    override fun onVisible() {
        super.onVisible()
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
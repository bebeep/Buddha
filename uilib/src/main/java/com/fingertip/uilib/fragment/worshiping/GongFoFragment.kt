package com.fingertip.uilib.fragment.worshiping

import android.view.View
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopFragment
import com.fingertip.uilib.R
import org.greenrobot.eventbus.Subscribe

/**
 * 供佛
 */
class GongFoFragment : TopFragment() {
    override fun layoutId(): Int = R.layout.fragment_gf

    override fun initShiTu() {


    }


    override fun getClickViews(): List<View> {
        val v = requireView()
        return listOf(v.findViewById(R.id.iv_back), v.findViewById(R.id.tv_gf))
    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)

    }


    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when(event.what) {

        }
    }
}
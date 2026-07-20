package com.fingertip.uilib.fragment.worshiping

import android.view.View
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentGfBinding
import org.greenrobot.eventbus.Subscribe

/**
 * 供佛
 */
class GongFoFragment : TopFragment() {
    override fun layoutId(): Int = R.layout.fragment_gf

    private val binding get() = mBinding as FragmentGfBinding

    override fun initShiTu() {


    }


    override fun getClickViews(): List<View> {
        return listOf(binding.ivBack, binding.tvGf)
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
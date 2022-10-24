package com.fingertip.baselib.event_bus

import org.greenrobot.eventbus.EventBus

object EventBusProxy {

    @JvmStatic
    fun register(obj: Any) {
        if (!EventBus.getDefault().isRegistered(obj)) {
            EventBus.getDefault().register(obj)
        }
    }

    @JvmStatic
    fun unRegister(obj: Any) {
        if (EventBus.getDefault().isRegistered(obj)) {
            EventBus.getDefault().unregister(obj)
        }
    }

    @JvmStatic
    fun post(obj: Any) {
        EventBus.getDefault().post(obj)
    }

    @JvmStatic
    fun postSticky(obj: Any) {
        EventBus.getDefault().postSticky(obj)
    }

    fun cancelEventDelivery(event: Any) {
        EventBus.getDefault().cancelEventDelivery(event)
    }
}
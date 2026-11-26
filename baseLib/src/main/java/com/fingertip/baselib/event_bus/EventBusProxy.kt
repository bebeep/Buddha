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

    /** 粘性事件消费后移除，避免订阅者下次注册时重复处理 */
    @JvmStatic
    fun removeStickyEvent(obj: Any) {
        EventBus.getDefault().removeStickyEvent(obj)
    }

    fun cancelEventDelivery(event: Any) {
        EventBus.getDefault().cancelEventDelivery(event)
    }
}
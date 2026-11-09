package com.fingertip.uilib.im

import com.fingertip.baselib.bean.WishRecordBean

/**
 * 许愿树跨页面事件：编辑页提交成功后把新愿望带给许愿树页，播放飞签动画。
 * 简单单例即可（同一时刻只有一条待飞愿望）。
 */
object WishBus {
    /** 编辑页刚提交成功的愿望，许愿树页消费后置空 */
    @Volatile
    var pendingNewWish: WishRecordBean? = null
}

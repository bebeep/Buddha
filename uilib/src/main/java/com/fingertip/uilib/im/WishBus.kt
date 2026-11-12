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

    /**
     * 被点击的占位牌挂点索引；null 表示来自底部【开始许愿】按钮。
     * - 非 null：飞签飞向该挂点，动画到达后移除该占位牌并替换。
     * - null：飞签随机飞向一个占位牌；若树上没有占位牌则替换最后一张。
     */
    @Volatile
    var pendingNewWishSlot: Int? = null
}

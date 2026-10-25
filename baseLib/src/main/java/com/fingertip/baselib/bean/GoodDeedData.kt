package com.fingertip.baselib.bean

/**
 * 行善数据，对应服务端 goodDeed 一组接口
 * 字段与 com.buddha.web.model.apiModel.GoodDeedRecord / DeedDonateRecord 对齐
 */

/**
 * 一条善行求助
 */
class DeedBean {
    var id: Int = 0
    var accountId: Int = 0
    var title: String? = null
    var content: String? = null
    //分类：走失/拐卖/资助/领养/支教/孤寡老人/留守儿童/动物保护
    var category: String? = null
    //0用户发布 1官方发布
    var isOfficial: Int = 0
    //0普通 1急
    var isUrgent: Int = 0
    //目标功德币
    var targetCoins: Int = 0
    //已捐功德币
    var donatedCoins: Int = 0
    //捐赠人数
    var donorCount: Int = 0
    //分享次数
    var shareCount: Int = 0
    //0进行中 1已达成 2打款中 3已完成
    var status: Int = 0
    //截止日期
    var deadline: String? = null
    //发布时间
    var createDate: String? = null
    var accountInfo: WishUserBean? = null
    //我已捐赠次数
    var myDonateCount: Int = 0
    //我已捐赠功德币
    var myDonateCoins: Int = 0

    fun isDeleted(): Boolean = status < 0
    fun isInProgress(): Boolean = status == 0
}

/**
 * 一条行善捐赠记录
 */
class DeedDonateBean {
    var id: Int = 0
    var deedId: Int = 0
    var accountId: Int = 0
    var coins: Int = 0
    var message: String? = null
    var createDate: String? = null
    var accountInfo: WishUserBean? = null
}

/** 行善分类标签 */
val DEED_CATEGORIES = listOf("走失", "拐卖", "资助", "领养", "支教", "孤寡老人", "留守儿童", "动物保护")

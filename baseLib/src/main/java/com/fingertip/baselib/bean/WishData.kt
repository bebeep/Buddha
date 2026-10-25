package com.fingertip.baselib.bean

/**
 * 许愿树数据，对应服务端 wishingTree 一组接口
 * 字段与 com.buddha.web.model.apiModel.WishingRecord / BlessRecord、SimpleAccount 对齐
 */

/**
 * 简要用户信息，对应服务端 SimpleAccount
 */
class WishUserBean {
    var accountId: Int = 0
    var displayAccountId: String? = null
    var userName: String? = null
    var avatar: String? = null
    //0 未知 1 男 2 女
    var gender: Int = 0
    var userLevel: Int = 0
}

/**
 * 一条愿望，对应服务端 WishingRecord
 */
class WishRecordBean {
    var id: Int = 0
    var accountId: Int = 0
    var wishContent: String? = null
    //0 未还愿 1 已还愿
    var wishStatus: Int = 0
    //审核结果：0 正常 1 违规 2 删除
    var reviewStatus: Int = 0
    //送出祝福的用户数量
    var blessUserCount: Int = 0
    //许愿时间 yyyy-MM-dd HH:mm:ss
    var createDate: String? = null

    fun isDeleted(): Boolean = reviewStatus == 2
    fun isIllegal(): Boolean = reviewStatus == 1
}

/**
 * 一条祝福，对应服务端 BlessRecord。
 * wishingAccountInfo 是许愿人、blessAccountInfo 是祝福人（本人时服务端直接给）
 */
class BlessRecordBean {
    var id: Int = 0
    var wishingId: Int = 0
    //祝福记录状态：0 正常 1 违规 2 删除
    var wishingStatus: Int = 0
    var wishingAccountInfo: WishUserBean? = null
    var blessAccountInfo: WishUserBean? = null
    var wishContent: String? = null
    var blessContent: String? = null
    //许愿时间 yyyy-MM-dd HH:mm:ss
    var wishingDate: String? = null
    //祝福时间 yyyy-MM-dd HH:mm:ss
    var blessDate: String? = null
}

package com.fingertip.baselib.bean

/**
 * 祈福数据，对应服务端 blessing 一组接口
 * 字段与 com.buddha.web.model.apiModel.PrayingRecord 对齐
 */

/**
 * 一张祈福福卡
 */
class PrayingBean {
    var id: Int = 0
    var accountId: Int = 0
    //祈福内容，20字内
    var content: String? = null
    //祈福卡封面 0默认
    var coverType: Int = 0
    //被祝福次数
    var blessCount: Int = 0
    //审核结果：0 正常 1 违规 2 删除
    var reviewStatus: Int = 0
    //发布时间 yyyy-MM-dd HH:mm:ss
    var createDate: String? = null
    var accountInfo: WishUserBean? = null
    //最近祝福的用户昵称，最多5个
    var blessUserNames: List<String>? = null
    //我是否已祝福过该福卡
    var myBlessed: Boolean = false

    fun isDeleted(): Boolean = reviewStatus == 2
}

/**
 * 今日祝福状态：data 直接是今日已祝福次数，上限5次
 */
const val PRAYING_DAILY_BLESS_LIMIT = 5
const val PRAYING_COVER_COINS = 5
const val PRAYING_BLESS_EXP = 5

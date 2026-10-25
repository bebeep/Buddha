package com.fingertip.baselib.bean

/**
 * 功德捐赠数据，对应服务端 donate 一组接口
 * 字段与 com.buddha.web.model.apiModel.DonateBuddhaRsp / DonateRecord / LuckyBagRsp 对齐
 */

/**
 * 功德佛像
 */
class DonateBuddhaBean {
    var id: Int = 0
    var buddhaName: String? = null
    var buddhaIntro: String? = null
    var buddhaCoverUrl: String? = null
    //在供人数
    var ownCount: Int = 0
    //累计功德
    var totalIncome: Int = 0
    //我的累计功德
    var myTotalContribution: Int = 0
    //我今日功德
    var myTodayContribution: Int = 0
    //我的贡献排名
    var myContributionRank: Int = 0
    //贡献榜前3头像
    var top3ContributionUserAvatar: List<String>? = null
    var buddhaModuleUrl: String? = null
    var buddha3DModuleUrl: String? = null
    var buddha3DModuleName: String? = null
    //今日剩余福袋数
    var todayRewardCount: Int = 0
}

/**
 * 一条捐赠记录
 */
class DonateRecordBean {
    var id: Int = 0
    var accountId: Int = 0
    var buddhaId: Int = 0
    var buddhaName: String? = null
    var buddhaCoverUrl: String? = null
    var coin: Int = 0
    var createDate: String? = null
}

/**
 * 福袋领取结果
 */
class LuckyBagBean {
    //实际获得功德币
    var rewardCoin: Int = 0
    //命中奖池的功德币
    var hitRewardCoin: Int = 0
    //领取后余额
    var currCoin: Int = 0
}

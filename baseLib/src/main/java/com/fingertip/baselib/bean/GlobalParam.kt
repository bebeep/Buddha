package com.fingertip.baselib.bean

class GlobalParam: TopData() {
    var maxAccount: Int? = 0

    //"每个设备创建账号的最小数量"/
    var minAccount : Int? = 0

    //("wxPayEnable")
    var wxPayEnable : Int? = 0

    //("aliPayEnable")
    var aliPayEnable : Int? = 0

    //("免费游戏次数")
    var freeGameTimesPerDay : Int? = 0

    //("每日福袋可开启次数")
    var buddhaRewardCountPerDay : Int? = 0

    //("奖池福袋金币分成比例")
    var buddhaRewardRate : Int? = 0

    //("单次中奖最小金额")
    var buddhaRewardOnceMinCoin : Int? = 0

    //("单次中奖最大金额")
    var buddhaRewardOnceMaxCoin : Int? = 0

    //("oss文件上传地址endPoint")
    var ossFileEndPoint : String? = ""

    //("oss文件上传地址bucketName")
    var bucketName : String? = ""

    //修行攻略等 H5 页面地址，由服务端下发热更新
    var guideUrl : String? = ""

    //佛像配置
    var buddhaConfig : List<BuddhaConfig>?=null

    //修行等级配置（user_level_config 全表，按 level 升序），头衔/徽章/经验门槛都从这里取
    var userLevelConfig : List<UserLevelConfig>?=null

    // 启动页整图地址：服务端下发，客户端缓存到本地 filesDir，下次启动直接显示（避免联网等待/首帧空白）
    var splashUrl : String? = ""

    // 启动页主色（可选）：兜底背景 bg_startup 主色与之匹配时过渡更无感
    var splashBgColor : String? = ""

    // 启动页缓存有效期（天）：到点或 url 变化才重新下载
    var splashExpireDays : Int? = 7
}
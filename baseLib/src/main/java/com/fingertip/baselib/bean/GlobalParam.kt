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
}
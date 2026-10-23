package com.fingertip.baselib.bean

/**
 * 签到信息，服务端按 sign_in_* 四张表现算出来
 */
class SignInData {
    //今天签过没有
    var hasSignIn: Boolean = false
    //当前已连续签到天数
    var continuousDays: Int = 0
    //历史最大连续签到天数
    var maxContinuousDays: Int = 0
    //累计签到天数
    var totalSignDays: Int = 0
    //每次签到都给的基础奖励
    var signInRewards: List<SignInRewardItem>? = null
    //下一档连续签到奖励；所有档位都拿过时是 null
    var nextContinuousReward: SignInContinuousReward? = null

    /**
     * 基础奖励里指定某类的数值，没有这类奖励就是 0
     */
    fun basicRewardValue(rewardType: Int): Int {
        return signInRewards?.firstOrNull { it.rewardType == rewardType }?.rewardValue ?: 0
    }

    /**
     * 基础奖励一句话说明，如「金币 +1、修行值 +1」
     */
    fun basicRewardText(): String {
        val list = signInRewards ?: return ""
        return list.joinToString("、") { "${it.rewardName} +${it.rewardValue}" }
    }
}

/**
 * 单条奖励，金币和修行值都是这个结构，以后再加新类型前端不用改
 */
class SignInRewardItem {
    //1 金币 2 修行值
    var rewardType: Int = 0
    //奖励名，服务端拼好了，前端直接显示
    var rewardName: String = ""
    var rewardValue: Int = 0

    fun display(): String = "$rewardName +$rewardValue"
}

/**
 * 连续签到达标的档位，比如连签 30 天这一档
 */
class SignInContinuousReward {
    //这一档要连签多少天
    var continuousDays: Int = 0
    //当前还差几天
    var remainDays: Int = 0
    //同一档可能配了金币、修行值多条
    var rewards: List<SignInRewardItem>? = null

    fun rewardText(): String {
        val list = rewards ?: return ""
        return list.joinToString("、") { "${it.rewardName} +${it.rewardValue}" }
    }

    /**
     * 当前进度的百分比，用于画进度条
     */
    fun progress(currentDays: Int): Int {
        if (continuousDays <= 0) return 0
        return (currentDays * 100 / continuousDays).coerceIn(0, 100)
    }
}

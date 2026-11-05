package com.fingertip.baselib.bean

/**
 * 修行等级配置，对应服务端 user_level_config 表
 *
 * exp 是"达到该等级所需的累计总经验"，不是升级增量。
 * 数据由 globalParam 一次下发（GlobalConfig.globalParam.userLevelConfig），
 * 全 App 共享一份，读的时候统一走 LevelUtil。
 */
class UserLevelConfig {
    var id: Int = 0
    var level: Int = 0
    var exp: Int = 0

    //等级头衔，如「三阶·善信」
    var levelTag: String? = ""

    //等级背景框图片地址（等级徽章底图）。为空时客户端按境界段用本地配色
    var tagUrl: String? = ""
}

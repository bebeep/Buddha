package com.fingertip.baselib.bean

/**
 * 拉黑记录，对应服务端 user_block_record（userTool/getBlockList）。
 *
 * targetUserName / targetAvatar 由服务端 LEFT JOIN account_info 带出，
 * 「设置 - 已拉黑用户」要能认出拉黑的是谁，只给 accountId 用户没法判断该不该解除。
 * 这两个字段是展示用的冗余，别拿去做业务判断。
 */
class UserBlockData {
    var id: Int = 0
    //发起拉黑的账号（自己）
    var accountId: Int = 0
    //被拉黑的账号
    var targetAccountId: Int = 0
    var createDate: String? = null

    //被拉黑者的昵称 / 头像
    var targetUserName: String? = null
    var targetAvatar: String? = null
}

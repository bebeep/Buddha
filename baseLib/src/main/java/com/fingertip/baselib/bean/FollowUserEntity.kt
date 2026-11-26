package com.fingertip.baselib.bean

/**
 * 粉丝 / 关注列表里的用户卡片，服务端 FollowUserRsp 对应。
 * 独立于搜索结果的 SearchUserEntity：这里只放关系列表展示必需的基本信息，
 * 后续两处各改各的字段，互不影响。
 */
class FollowUserEntity {
    var accountId: Int = 0
    var displayAccountId: String? = null
    var userName: String? = null
    var avatar: String? = null
    var mood: String? = null
    var userLevel: Int = 0
}

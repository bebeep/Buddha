package com.fingertip.baselib.bean

/**
 * 搜索结果里的佛友卡片，服务端 SearchUserRsp 对应。
 * 只含展示字段，不回传 session / 邮箱等敏感信息。
 */
class SearchUserEntity {
    var accountId: Int = 0
    var displayAccountId: String? = null
    var userName: String? = null
    var avatar: String? = null
    var mood: String? = null
    var userLevel: Int = 0
    var roleType: Int = 0
    var followerCount: Int = 0
    var followingCount: Int = 0
}

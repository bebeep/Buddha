package com.fingertip.baselib.bean

/**
 * 寺庙数据，对应服务端 temple 一组接口
 * 字段与 com.buddha.web.model.apiModel.TempleRsp / TempleCommentRsp 对齐
 */

/**
 * 寺庙
 */
class TempleBean {
    var id: Int = 0
    var templeName: String? = null
    //称号，多个用,隔开
    var templeTitle: String? = null
    //封面，多张逗号隔开
    var templeImages: String? = null
    var templeIntro: String? = null
    var templeHistory: String? = null
    var templeAddr: String? = null
    var latitude: Float = 0f
    var longitude: Float = 0f
    var recommendUp: Int = 0
    var recommendDown: Int = 0
    var commentCount: Int = 0
    //距离：米
    var distance: Int = 0
    //我的推荐状态：0未推荐 1推荐 2不推荐
    var myRecommendStatus: Int = 0

    fun coverList(): List<String> =
        templeImages?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    fun distanceText(): String = when {
        distance <= 0 -> ""
        distance < 1000 -> "${distance}m"
        else -> String.format("%.1fkm", distance / 1000f)
    }
}

/**
 * 寺庙评论
 */
class TempleCommentBean {
    var id: Int = 0
    var accountId: Int = 0
    var avatar: String? = null
    var nickName: String? = null
    var userLevel: Int = 0
    var commentDate: String? = null
    var content: String? = null
}

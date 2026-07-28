package com.fingertip.baselib.bean


/**
 * 动态
 */
class MomentEntity: TopData() {

    //"动态Id"
    var momentId = 0

    //"发布者账号id"
    var postAccountId = 0

    //"发布者昵称"
    var postUserName: String? = null

    //"发布者头像"
    var postAvatar: String? = null

    //"发布者等级"
    var userLevel = 0

    //"发布者角色类型：1：普通用户 2：认证大师"
    var roleType = 0

    //"动态类型（1：文本 2：视频 3 图片）"
    var momentType = 0

    //"是否置顶"
    var isTop = false

    //"是否匿名"
    var isAnonymous = false

    //"文本内容"
    var textContent: String? = null

    //"视频资源"
    var videoUrl: String? = null

    //"视频时长"
    var videoDuration = 0

    //"视频封面"
    var videoCover: String? = null

    //"视频宽"
    var videoWidth = 0

    //"视频高"
    var videoHeight = 0

    //"图片地址合集"
    var imageUrl: List<String?> = ArrayList()

    //"点赞数"
    var likeCount = 0

    //"浏览量"
    var viewCount = 0

    //"评论数"
    var commentCount = 0

    //"分享次数"
    var shareCount = 0

    //"创建时间"
    var createDate: String? = ""

    //"是否已关注"
    var isFollowed = false

    //"是否已点赞"
    var isLiked = false


}
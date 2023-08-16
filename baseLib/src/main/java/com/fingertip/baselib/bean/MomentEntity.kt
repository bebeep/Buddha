package com.fingertip.baselib.bean


/**
 * 动态
 */
class MomentEntity: TopData() {

    var momentId = -1                // 动态Id
    var avatar = ""                // 头像
    var lineStatus = 0        // 线上状态
    var nickName = ""        // 昵称
    var gender = ""                // 性别
    var age = 18                        // 年龄
    var accountId= 0                // 账号Id
    var roleType = 0                // 角色类型（1：用户，2：主播）
    var tags = ArrayList<Int>()// 标签 （1：NEW，2：HOT，3：官方）
    var momentType = 1        // 动态类型（1：文字，2：图片，3：视频）
    var content = ""                // 文字
    var images = ArrayList<String?>() // 图片
    var imageWidths = ArrayList<Int>() // 图片宽
    var imageHeights = ArrayList<Int>() // 图片高
    var video = ""             // 视频
    var videoCover = ""               // 视频封面
    var videoWidth = 0                // 视频宽
    var videoHeight = 0        // 视频高
    var likes = 0                // 点赞数
    var commments = 0        // 评论数
    var isTop = false                // 是否置顶
    var isLiked = false                // 是否已点赞
    var isFollowed = false        // 是否已关注
    var createTime = 0        // 发布时间（从UTC时区 2017-01-01 00:00:00 开始到现在的秒数）



}
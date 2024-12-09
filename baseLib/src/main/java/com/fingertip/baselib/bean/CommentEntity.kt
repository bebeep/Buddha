package com.fingertip.baselib.bean


/**
 * 评论
 */
class CommentEntity : TopData(){

    var momentId = 0                // 动态Id
    var commentId = 0        // 评论Id
    var senderAvatar = ""// 发布者头像
    var senderNickname= ""// 发布者昵称
    var senderAccountId=0        // 发布者账号Id
    var senderRoleType=1                // 发布者角色类型（1：用户，2：主播）

    var receiverAvatar = ""// 发布者头像
    var receiverNickname=""// 发布者昵称
    var receiverAccountId=0        // 发布者账号Id
    var receiverRoleType=1                // 发布者角色类型（1：用户，2：主播）

    var content = ""                // 文字
    var momentThumbnail=""        // 动态缩略图
    var commentTime = 0                // 评论时间（从UTC时区 2017-01-01 00:00:00 开始到现在的秒数）
    var momentOwner = 0 //动态拥有者的accountId
    var interactionType = 1 //1评论  2点赞


    var expandComments = ArrayList<CommentEntity>()       // 子评论
}
package com.fingertip.baselib.bean


/**
 * 动态评论
 */
class CommentEntity: TopData() {
    //("评论id")
    var id = 0

    //("评论者id")
    var senderAccountId = 0

    //("评论者等级")
    var senderLevel = 0

    //("评论者昵称")
    var senderNickName: String? = ""

    //("评论者头像")
    var senderAvatar: String? = ""

    //("动态id")
    var momentId = 0

    //("主评论id，为0时表示自己就是主评论")
    var parentCommentId = 0

    //("评论类型（1：文本 2 图片 ）")
    var commentType = 0

    //("点赞数")
    var likeCount = 0

    //("文本内容）")
    var textContent: String? = ""

    //("图片内容）")
    var imageUrl: String? = ""

    //("被回复人昵称-仅在子评论中出现")
    var replyNickName: String? = ""

    //("是否已删除")
    var isDeleted = false

    //("评论时间")
    var commentDate: String? = ""

    //("子评论数量")
    var childCommentCount = 0

    //("子评论列表")
    var childComment: ArrayList<CommentEntity>? = null


}
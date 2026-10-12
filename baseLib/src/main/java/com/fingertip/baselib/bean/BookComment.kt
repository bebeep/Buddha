package com.fingertip.baselib.bean

class BookComment: TopData() {
    //评论id
    var id = 0

    //经书id
    var bookId = 0

    //账号id
    var accountId = 0

    //昵称
    var userName = ""

    //性别 0 未知 1 男 2女
    var gender = 0

    //头像
    var avatar = ""

    //等级
    var userLevel = 0

    //评论内容
    var textContent = ""

    //是否大师推荐
    var isMasterRecommend = false

    //是否已点赞
    var isLiked = false

    //点赞数
    var likeCount = 0

    //更新时间"
    var createDate = "2027-01-01 00:00:00"


}
package com.fingertip.baselib.net

import com.fingertip.baselib.BuildConfig


object NetProperty {


    /**
     * 平台：1 安卓 2 iOS
     */
    const val PLATFORM = 1

    /**
     * 渠道号
     */
    const val CHANNEL = 1

    /**
     * 接口签名key
     */
    const val API_KEY = "a8010cb1539eaa049908fc2de4184e64"


    /**
     * 测试服
     */
    var TEST_URL            = "http://192.168.10.195:8888/buddha/"   //公司
//    var TEST_URL            = "http://192.168.1.249:8888/buddha/"   //家里
//    var TEST_URL            = "https://soonturn.com/buddha"

    /**
     * 正式服
     */
    var PROD_URL            = "https://soonturn.com/buddha/"


    var SERVER_URL: String
        get() {
            return  if (BuildConfig.DEBUG) TEST_URL else PROD_URL
        }
        set(value) {}

    /**
     * 公共请求头
     */
    val BASE_URL: String
        get() {
            return SERVER_URL
        }



    /********************** 以下是接口 **********************/




    /**
     * 检查服务器状态
     * 获取更新信息、全局参数
     */
    const val CHECK_SERVER_STATUS             = "config/checkUpdate"

    /**
     * 获取oss配置
     */
    const val GET_OSS_CONFIG                  = "config/getOssConfig"


    /**
     * 登录
     */
    const val LOGIN                           = "account/login"


    /**
     * 登出
     */
    const val LOGOUT                          = "account/logout"



    /**
     * 发布动态
     */
    const val POST_MOMENT                     = "moment/postMoment"


    /**
     * 获取动态列表-HOT
     */
    const val GET_MOMENT_LIST_HOT                     = "moment/getMomentListHot"


    /**
     * 获取动态列表-FOLLOWED
     */
    const val GET_MOMENT_LIST_FOLLOWED                     = "moment/getMomentListFollowed"


    /**
     * 获取动态详情
     */
    const val GET_MOMENT_DETAILS                     = "moment/getMomentDetails"


    /**
     * 获取评论列表
     */
    const val GET_MOMENT_COMMENT_LIST                    = "moment/getCommentByMomentId"


    /**
     * 获取评论详情
     */
    const val  GET_MOMENT_COMMENT_DETAILS                    = "moment/getCommentDetails"


    /**
     * 提交动态评论
     */
    const val COMMENT_MOMENT                    = "moment/commentMoment"


    /**
     * 设为已读
     */
    const val VIEW_MOMENT                    = "moment/viewMoment"

    /**
     * 点赞动态
     */
    const val LIKE_MOMENT                    = "moment/likeMoment"

    /**
     * 取消点赞动态
     */
    const val UN_LIKE_MOMENT                    = "moment/unLikeMoment"

    /**
     * 点赞评论
     */
    const val LIKE_MOMENT_COMMENT                    = "moment/likeComment"

    /**
     * 取消点赞评论
     */
    const val UN_LIKE_MOMENT_COMMENT                     = "moment/unLikeComment"


    /**
     * 取消点赞评论
     */
    const val GET_BUDDHA_CONFIG                     = "account/getBuddhaConfig"

    /**
     * 获取经书类型
     */
    const val GET_BOOK_TYPE_LIST                     = "book/getBookTypeConfig"

    /**
     * 获取经书主题列表
     */
    const val GET_BOOK_SUBJECT_LIST                     = "book/getBookThemeConfig"

    /**
     * 根据主题获取经书
     */
    const val GET_BOOK_LIST_BY_THEME                    = "book/getBookByTheme"

    /**
     * 根据分类获取经书
     */
    const val GET_BOOK_LIST_BY_TYPE                    = "book/getBookByType"

    /**
     * 获取书架上的书
     */
    const val GET_BOOK_LIST_IN_SHELF                    = "book/getBookInShelf"
    /**
     * 获取书籍详情
     */
    const val GET_BOOK_INFO                    = "book/getBookInfo"
    /**
     * 获取书籍评论
     */
    const val GET_BOOK_COMMENT                    = "book/getBookComment"
    /**
     * 点赞书籍评论
     */
    const val LIKE_BOOK_COMMENT                    = "book/likeBookComment"
    /**
     * 取消点赞书籍评论
     */
    const val UN_LIKE_BOOK_COMMENT                    = "book/unLikeBookComment"

    /**
     * 加入书架
     */
    const val ADD_TO_BOOK_SHELF                       = "book/addToBookShelf"

    /**
     * 移出书架
     */
    const val REMOVE_FROM_BOOK_SHELF                  = "book/removeFromBookShelf"

    /**
     * 添加书籍评论
     */
    const val ADD_BOOK_COMMENT                        = "book/addBookComment"

    /**
     * 删除书籍评论
     */
    const val REMOVE_BOOK_COMMENT                     = "book/removeBookComment"

    /**
     * 获取章节目录
     */
    const val GET_BOOK_CHAPTER_LIST                   = "book/getBookChapterList"

    /**
     * 获取单章正文
     */
    const val GET_BOOK_CHAPTER                        = "book/getBookChapter"

    /**
     * 批量获取正文（整部离线下载，一次最多 20 章）
     */
    const val GET_BOOK_CHAPTERS                       = "book/getBookChapters"

    /**
     * 获取读经抄经进度
     */
    const val GET_BOOK_PROGRESS                       = "book/getBookProgress"

    /**
     * 更新读经进度
     */
    const val UPDATE_READ_PROGRESS                    = "book/updateReadProgress"

    /**
     * 更新抄经进度
     */
    const val UPDATE_COPY_PROGRESS                    = "book/updateCopyProgress"

    /**
     * 获取抄经显示设置
     */
    const val GET_COPY_SETTING                        = "book/getCopySetting"

    /**
     * 保存抄经显示设置
     */
    const val UPDATE_COPY_SETTING                     = "book/updateCopySetting"

    /**
     * 获取读经显示设置
     */
    const val GET_READ_SETTING                        = "book/getReadSetting"

    /**
     * 保存读经显示设置
     */
    const val UPDATE_READ_SETTING                     = "book/updateReadSetting"
}

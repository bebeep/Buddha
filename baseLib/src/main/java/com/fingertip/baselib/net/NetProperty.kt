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
//    var TEST_URL            = "http://192.168.10.195:8888/buddha/"   //公司
    var TEST_URL            = "http://192.168.1.249:8888/buddha/"   //家里
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

    /**
     * 获取签到信息
     */
    const val GET_SIGN_IN_DATA                        = "account/getSignInData"

    /**
     * 签到
     */
    const val SIGN_IN                                 = "account/signIn"

    /**
     * 获取月历（含整月签到状态与今日宜忌详情），仅限当前时间前后一年
     */
    const val GET_MONTH_CALENDAR                      = "account/getMonthCalendar"

    /**
     * 获取今日佛历信息（首页顶部佛历日期与今日佛事）
     */
    const val GET_TODAY_BUDDHA_INFO                   = "account/getTodayBuddhaInfo"

    /**
     * 获取许愿文本示例
     */
    const val GET_WISHING_TAG                         = "wishingTree/getWishingTag"

    /**
     * 许愿
     */
    const val POST_WISHING                            = "wishingTree/postWishing"

    /**
     * 删除许愿
     */
    const val DELETE_WISHING                          = "wishingTree/deleteWishing"

    /**
     * 添加祝福
     */
    const val BLESS_WISHING                           = "wishingTree/blessWishing"

    /**
     * 设置愿望已读
     */
    const val READ_WISHING                            = "wishingTree/readWishing"

    /**
     * 获取许愿树信息
     */
    const val GET_WISHING_TREE                        = "wishingTree/getWishingTree"

    /**
     * 获取我的许愿记录
     */
    const val GET_MY_WISHING_RECORD                   = "wishingTree/getMyWishingRecord"

    /**
     * 获取我的祝福记录
     */
    const val GET_MY_BLESS_RECORD                     = "wishingTree/getMyBlessRecord"

    /**
     * 根据愿望id获取祝福记录
     */
    const val GET_BLESS_RECORD_BY_WISHING_ID          = "wishingTree/getBlessRecordByWishingId"

    /**
     * 福卡墙
     */
    const val GET_PRAYING_WALL                        = "blessing/getPrayingWall"

    /**
     * 所有祈福（分页）
     */
    const val GET_ALL_PRAYING_RECORD                  = "blessing/getAllPrayingRecord"

    /**
     * 我的祈福（分页）
     */
    const val GET_MY_PRAYING_RECORD                   = "blessing/getMyPrayingRecord"

    /**
     * 发布祈福
     */
    const val POST_PRAYING                            = "blessing/postPraying"

    /**
     * 删除祈福
     */
    const val DELETE_PRAYING                          = "blessing/deletePraying"

    /**
     * 祝福福卡
     */
    const val BLESS_PRAYING                           = "blessing/blessPraying"

    /**
     * 今日已祝福次数
     */
    const val GET_PRAYING_STATUS                      = "blessing/getPrayingStatus"

    /**
     * 善行列表（分页）
     */
    const val GET_GOOD_DEED_LIST                      = "goodDeed/getGoodDeedList"

    /**
     * 善行详情
     */
    const val GET_GOOD_DEED_INFO                      = "goodDeed/getGoodDeedInfo"

    /**
     * 发布求助
     */
    const val POST_GOOD_DEED                          = "goodDeed/postGoodDeed"

    /**
     * 删除求助
     */
    const val DELETE_GOOD_DEED                        = "goodDeed/deleteGoodDeed"

    /**
     * 捐赠功德币
     */
    const val DONATE_GOOD_DEED                        = "goodDeed/donateGoodDeed"

    /**
     * 分享助力
     */
    const val SHARE_GOOD_DEED                         = "goodDeed/shareGoodDeed"

    /**
     * 善行捐赠记录（分页）
     */
    const val GET_DEED_DONATE_RECORD                  = "goodDeed/getDeedDonateRecord"

    /**
     * 我发布的求助（分页）
     */
    const val GET_MY_DEED_RECORD                      = "goodDeed/getMyDeedRecord"

    /**
     * 我捐赠的善行（分页）
     */
    const val GET_MY_DONATE_DEED_RECORD               = "goodDeed/getMyDonateDeedRecord"

    /**
     * 寺庙列表（分页）
     */
    const val GET_TEMPLE_LIST                         = "temple/getTempleList"

    /**
     * 寺庙详情
     */
    const val GET_TEMPLE_INFO                         = "temple/getTempleInfo"

    /**
     * 提交推荐 1推荐 2不推荐
     */
    const val POST_RECOMMEND_STATUS                   = "temple/postRecommendStatus"

    /**
     * 发表寺庙评论
     */
    const val POST_TEMPLE_COMMENT                     = "temple/postComment"

    /**
     * 寺庙评论列表（分页）
     */
    const val GET_TEMPLE_COMMENT                      = "temple/getTempleComment"

    /**
     * 功德佛像列表
     */
    const val GET_DONATE_BUDDHA_LIST                  = "donate/getDonateBuddhaList"

    /**
     * 功德佛像详情
     */
    const val GET_DONATE_BUDDHA_DETAIL                = "donate/getDonateBuddhaDetail"

    /**
     * 功德捐赠
     */
    const val DONATE_BUDDHA                           = "donate/donateBuddha"

    /**
     * 功德捐赠记录
     */
    const val GET_DONATE_RECORD                       = "donate/getDonateRecord"

    /**
     * 福袋领取
     */
    const val GAIN_LUCKY_BAG                          = "donate/gainLuckyBag"

    /**
     * 获取用户信息
     */
    const val GET_USER_INFO                           = "account/getUserInfo"

    /**
     * 修改个人资料
     */
    const val MODIFY_PROFILE                          = "account/modifyProfile"

    /**
     * 关注
     */
    const val FOLLOW                                  = "account/follow"

    /**
     * 取消关注
     */
    const val UN_FOLLOW                               = "account/unFollow"
}

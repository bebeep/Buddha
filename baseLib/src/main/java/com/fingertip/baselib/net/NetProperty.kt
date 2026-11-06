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
//    var TEST_URL            = "http://192.168.10.195:8888/buddha/"   //公司（本机 jar 跑 8899）
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
     * 登录（loginType=1 邮箱密码 2 Google 3 Apple）
     */
    const val LOGIN                           = "account/login"

    /**
     * 邮箱注册
     */
    const val ACCOUNT_REGISTER                = "account/register"

    /**
     * 邮箱是否可用
     */
    const val ACCOUNT_CHECK_EMAIL             = "account/checkEmail"

    /**
     * 绑定第三方账号 / 补绑邮箱
     */
    const val ACCOUNT_BIND_THIRD              = "account/bindThird"

    /**
     * 设置/修改密码（第三方注册的账号补密码）
     */
    const val ACCOUNT_SET_PASSWORD            = "account/setPassword"

    /**
     * session 续期（启动时自动登录）
     */
    const val ACCOUNT_REFRESH_SESSION         = "account/refreshSession"

    /**
     * 注册推送设备订阅
     */
    const val PUSH_REGISTER                   = "push/register"

    /**
     * 解绑推送设备订阅
     */
    const val PUSH_UNREGISTER                 = "push/unregister"

    /**
     * 账号级推送开关
     */
    const val PUSH_SWITCH                     = "push/switch"


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
    const val GET_MOMENT_LIST_NEW                          = "moment/getMomentListNew"
    const val GET_MOMENT_LIST_CHECKIN                      = "moment/getMomentListCheckIn"


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
     * 获取首页聚合数据（修持进度 + 供灯状态 + 功德币余额 + 连续天数 + 区块配置）
     */
    const val HOME_SUMMARY                            = "home/getSummary"

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

    /**
     * 供佛（选择供奉天数，扣功德币）
     */
    const val WORSHIP_BUDDHA                          = "account/worshipBuddha"


    /********************** 海外版：装扮商城 **********************/

    /**
     * 商城商品列表。dressType 1灯具 2佛龛 3头像框 4昵称3D 5灯油
     * 公开接口，游客可逛
     */
    const val DRESS_LIST                              = "dress/getList"

    /**
     * 我的装扮（持有列表 + 佩戴状态），需登录
     */
    const val DRESS_MY                                = "dress/getMy"

    /**
     * 佩戴/摘下装扮，需登录
     */
    const val DRESS_WEAR                              = "dress/wear"

    /**
     * 创建 IAP 订单（返回内部订单号）
     */
    const val DRESS_CREATE_ORDER                      = "dress/createOrder"

    /**
     * IAP 验单并发货
     */
    const val DRESS_VERIFY                            = "dress/verify"

    /**
     * 功德币购买装扮（APP 内唯一购买方式），成功返回剩余功德币
     */
    const val DRESS_BUY_COIN                          = "dress/buyByCoin"


    /********************** 海外版：功德币充值 **********************/

    /**
     * 充值档位列表（含 IAP 商品 id）
     */
    const val RECHARGE_CONFIG                         = "recharge/getConfig"

    /**
     * 创建充值订单，返回内部订单号
     */
    const val RECHARGE_CREATE_ORDER                   = "recharge/createOrder"

    /**
     * 充值验单到账，返回最新功德币余额
     */
    const val RECHARGE_VERIFY                         = "recharge/verify"


    /********************** 海外版：早课修持 **********************/

    /**
     * 提交早课/晚课（一人一天一条，服务端按 timeZone 判定早/晚课）
     */
    const val PRACTICE_SUBMIT                         = "practice/submit"

    /**
     * 早课连续统计（连续天数 / 最长连续 / 累计次数 / 已领里程碑）
     */
    const val PRACTICE_STATISTIC                      = "practice/getStatistic"

    /**
     * 念佛计数上报（每满 108 声发 1 功德币，单日单佛号封顶）
     */
    const val PRACTICE_MEDITATION_REPORT              = "practice/meditationReport"

    /**
     * 累计念佛总声数
     */
    const val PRACTICE_MEDITATION_TOTAL               = "practice/getMeditationTotal"


    /********************** 海外版：供灯 **********************/

    /**
     * 当前供灯状态（0无灯 1燃烧中 2将熄），查询前服务端会惰性收掉过期灯
     */
    const val LAMP_STATUS                             = "lamp/getStatus"

    /**
     * 供灯历史（点亮/续燃记录，每条绑 IAP 订单号）
     */
    const val LAMP_HISTORY                            = "lamp/getHistory"


    /********************** 海外版：账号（设置页） **********************/

    /**
     * 退出登录（服务端清 session）
     */
    const val ACCOUNT_LOGOUT                          = "account/logout"

    /**
     * 注销账号（Google Play 账号删除政策要求 App 内可发起）
     */
    const val ACCOUNT_DELETE                          = "account/deleteAccount"

    /**
     * 举报（UGC 合规件：动态/评论）
     */
    const val ACCOUNT_REPORT                          = "account/report"


    /********************** 海外版：拉黑（UGC 合规） **********************/

    /**
     * 拉黑用户
     */
    const val USERTOOL_ADD_BLOCK                      = "userTool/addBlock"

    /**
     * 取消拉黑
     */
    const val USERTOOL_CANCEL_BLOCK                   = "userTool/cancelBlock"

    /**
     * 我的拉黑列表
     */
    const val USERTOOL_BLOCK_LIST                     = "userTool/getBlockList"


    /********************** 海外版：收藏（佛经/感悟/寺庙统一收藏） **********************/

    /**
     * 收藏列表（targetType 1佛经 2感悟 3寺庙，0查全部）
     */
    const val USERTOOL_COLLECT_LIST                   = "userTool/getCollectList"

    /**
     * 添加收藏
     */
    const val USERTOOL_ADD_COLLECT                    = "userTool/addCollect"

    /**
     * 取消收藏
     */
    const val USERTOOL_CANCEL_COLLECT                 = "userTool/cancelCollect"

    /**
     * 是否已收藏（详情页初始化收藏按钮状态）
     */
    const val USERTOOL_IS_COLLECTED                   = "userTool/isCollected"


    /********************** 海外版：恢复购买（App Store 3.1.1 要求） **********************/

    /**
     * 按平台交易号补发已付款未发货的订单
     */
    const val DRESS_RESTORE                           = "dress/restore"


    /********************** 海外版：我的感悟（UGC 可删除自己的内容） **********************/

    /**
     * 某人的动态列表（targetAccountId 传 0 查自己）
     */
    const val MOMENT_LIST_BY_ACCOUNT                  = "moment/getMomentListByAccount"

    /**
     * 删除自己的动态（软删）
     */
    const val MOMENT_DELETE                           = "moment/deleteMoment"


    /********************** 海外版：佛历订阅提醒 **********************/

    /**
     * 提醒规则列表（首次调用服务端会补两条默认规则）
     */
    const val USERTOOL_REMIND_LIST                    = "userTool/getRemindList"

    /**
     * 保存提醒规则（id=0 新建，id>0 更新）
     */
    const val USERTOOL_REMIND_SAVE                    = "userTool/saveRemind"

    /**
     * 删除提醒规则
     */
    const val USERTOOL_REMIND_DELETE                  = "userTool/deleteRemind"

    /**
     * 未来一段时间的佛历节日（提醒页预告列表）
     */
    const val USERTOOL_FESTIVAL_LIST                  = "userTool/getFestivalList"

    /**
     * 保存最近选中的佛像（拜佛页切换时上报，登录后各端恢复）
     */
    const val ACCOUNT_SAVE_LAST_BUDDHA                = "account/saveLastBuddha"

    /**
     * 游客登录（服务端按设备号建/找回游客号，享正式账号全部功能）
     */
    const val ACCOUNT_TRY_PLAY                        = "account/tryPlay"

    /**
     * 腾讯云 IM 登录凭证（service 端签 userSig，密钥不下发客户端）
     */
    const val ACCOUNT_IM_USER_SIG                     = "account/getImUserSig"

    /**
     * 发起会话前导入对方 IM 账号（幂等；对方从未登录过 IM 时发消息会 6032）
     */
    const val IM_IMPORT_PEER                          = "im/importPeer"

    /**
     * 私有 OSS 文件换取带签名的临时访问 URL（IM 媒体消息渲染用）
     */
    const val CONFIG_GET_ACCESS_URL                   = "config/getAccessUrl"
}

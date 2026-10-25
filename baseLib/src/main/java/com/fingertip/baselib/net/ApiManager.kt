package com.fingertip.baselib.net

import com.fingertip.baselib.bean.BookChapter
import com.fingertip.baselib.bean.BookChapterItem
import com.fingertip.baselib.bean.BookComment
import com.fingertip.baselib.bean.BookCopySetting
import com.fingertip.baselib.bean.BookProgress
import com.fingertip.baselib.bean.BookReadSetting
import com.fingertip.baselib.bean.BookThemeEntity
import com.fingertip.baselib.bean.BookTypeConfig
import com.fingertip.baselib.bean.BuddhaBook
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.bean.OssConfig
import com.fingertip.baselib.bean.CalendarSimpleRsp
import com.fingertip.baselib.bean.BlessRecordBean
import com.fingertip.baselib.bean.WishRecordBean
import com.fingertip.baselib.bean.PrayingBean
import com.fingertip.baselib.bean.DeedBean
import com.fingertip.baselib.bean.DeedDonateBean
import com.fingertip.baselib.bean.TempleBean
import com.fingertip.baselib.bean.TempleCommentBean
import com.fingertip.baselib.bean.DonateBuddhaBean
import com.fingertip.baselib.bean.DonateRecordBean
import com.fingertip.baselib.bean.LuckyBagBean
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.bean.RequestRsp
import com.fingertip.baselib.bean.SignInData
import com.fingertip.baselib.bean.VersionInfo
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.util.DeviceIdUtils
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiManager {



    /**
     * 检查服务器状态
     */
    @POST()
    suspend fun checkServerStatus(
        @Url url: String = NetProperty.CHECK_SERVER_STATUS,
        @Query("channelId") channelId: Int = NetProperty.CHANNEL,
        @Query("appVersion") appVersion: String = DeviceIdUtils.getVersionName()
    ): RequestRsp<VersionInfo?>

    /**
     * 获取oss配置
     */
    @GET()
    suspend fun getOssConfig(
        @Url url: String = NetProperty.GET_OSS_CONFIG,
        @Query("session") session: String = GlobalConfig.session,
    ): RequestRsp<OssConfig?>

    /**
     * 登录
     */
    @POST()
    suspend fun login(
        @Url url: String = NetProperty.LOGIN,
        @Body request: RequestBody?
    ): RequestRsp<PersonData?>

    /**
     * 发布动态
     */
    @POST()
    suspend fun postMoment(
        @Url url: String = NetProperty.POST_MOMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody?
    ): RequestRsp<String>

    /**
     * 动态列表-HOT
     */
    @GET()
    suspend fun getMomentListHot(
        @Url url: String = NetProperty.GET_MOMENT_LIST_HOT,
        @Query("session") session: String = GlobalConfig.session,
        @Query("pageCount") pageCount: Int = 0,
    ): RequestRsp<List<MomentEntity>>

    /**
     * 动态列表-FOLLOWED
     */
    @GET()
    suspend fun getMomentListFollowed(
        @Url url: String = NetProperty.GET_MOMENT_LIST_FOLLOWED,
        @Query("session") session: String = GlobalConfig.session,
        @Query("pageCount") pageCount: Int = 0,
    ): RequestRsp<List<MomentEntity>>

    /**
     * 动态详情
     */
    @GET()
    suspend fun getMomentDetails(
        @Url url: String = NetProperty.GET_MOMENT_DETAILS,
        @Query("session") session: String = GlobalConfig.session,
        @Query("momentId") momentId: Int = 0,
    ): RequestRsp<MomentEntity>

    /**
     * 动态详情
     */
    @GET()
    suspend fun getCommentByMomentId(
        @Url url: String = NetProperty.GET_MOMENT_COMMENT_LIST,
        @Query("session") session: String = GlobalConfig.session,
        @Query("momentId") momentId: Int = 0,
        @Query("pageCount") pageCount: Int = 0,
    ): RequestRsp<List<CommentEntity>>

    /**
     * 动态详情
     */
    @GET()
    suspend fun getCommentDetails(
        @Url url: String = NetProperty.GET_MOMENT_COMMENT_DETAILS,
        @Query("session") session: String = GlobalConfig.session,
        @Query("commentId") commentId: Int = 0,
        @Query("pageCount") pageCount: Int = 0,
    ): RequestRsp<List<CommentEntity>>

    /**
     * 提交动态评论
     */
    @POST()
    suspend fun commentMoment(
        @Url url: String = NetProperty.COMMENT_MOMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody?
    ): RequestRsp<CommentEntity>

    /**
     * 提交动态评论
     */
    @POST()
    suspend fun viewMoment(
        @Url url: String = NetProperty.VIEW_MOMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody?
    ): RequestRsp<CommentEntity>

    /**
     * 动态点赞
     */
    @POST()
    suspend fun likeMoment(
        @Url url: String = NetProperty.LIKE_MOMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Query("momentId") momentId: Int,
    ): RequestRsp<String>

    /**
     * 取消动态点赞
     */
    @POST()
    suspend fun unLikeMoment(
        @Url url: String = NetProperty.UN_LIKE_MOMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Query("momentId") momentId: Int,
    ): RequestRsp<String>

    /**
     * 评论点赞
     */
    @POST()
    suspend fun likeMomentComment(
        @Url url: String = NetProperty.LIKE_MOMENT_COMMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Query("commentId") commentId: Int,
    ): RequestRsp<String>

    /**
     * 取消评论点赞
     */
    @POST()
    suspend fun unLikeMomentComment(
        @Url url: String = NetProperty.UN_LIKE_MOMENT_COMMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Query("commentId") commentId: Int,
    ): RequestRsp<String>

    /**
     * 获取佛像列表
     */
    @POST()
    suspend fun getBuddhaConfig(
        @Url url: String = NetProperty.GET_BUDDHA_CONFIG,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<BuddhaConfig>>

    /**
     * 获取经书类型
     */
    @GET()
    suspend fun getBookTypeList(
        @Url url: String = NetProperty.GET_BOOK_TYPE_LIST,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<BookTypeConfig>>

    /**
     * 获取经书主题列表
     */
    @GET()
    suspend fun getBookThemeList(
        @Url url: String = NetProperty.GET_BOOK_SUBJECT_LIST,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<BookThemeEntity>>

    /**
     * 根据主题id获取经书
     */
    @GET()
    suspend fun getBookByTheme(
        @Url url: String = NetProperty.GET_BOOK_LIST_BY_THEME,
        @Query("session") session: String = GlobalConfig.session,
        @Query("themeId") themeId: Int,
        @Query("currPage") currPage: Int,
    ): RequestRsp<List<BuddhaBook>>

    /**
     * 根据分类id获取经书
     */
    @GET()
    suspend fun getBookByType(
        @Url url: String = NetProperty.GET_BOOK_LIST_BY_TYPE,
        @Query("typeId") typeId: Int,
        @Query("currPage") currPage: Int,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<BuddhaBook>>

    /**
     * 获取书架上的书
     */
    @GET()
    suspend fun getBookInShelf(
        @Url url: String = NetProperty.GET_BOOK_LIST_IN_SHELF,
        @Query("currPage") currPage: Int,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<BuddhaBook>>

    /**
     * 获取书籍详情
     */
    @GET()
    suspend fun getBookInfoByBookId(
        @Url url: String = NetProperty.GET_BOOK_INFO,
        @Query("bookId") bookId: Int,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<BuddhaBook>
    /**
     * 获取书籍评论
     */
    @GET()
    suspend fun getBookComment(
        @Url url: String = NetProperty.GET_BOOK_COMMENT,
        @Query("bookId") bookId: Int,
        @Query("currPage") currPage: Int,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<BookComment>>

    /**
     * 点赞书籍评论
     */
    @POST()
    suspend fun likeBookComment(
        @Url url: String = NetProperty.LIKE_BOOK_COMMENT,
        @Query("bookId") bookId: Int,
        @Query("commentId") commentId: Int,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<String>
    /**
     * 取消点赞书籍评论
     */
    @POST()
    suspend fun unLikeBookComment(
        @Url url: String = NetProperty.UN_LIKE_BOOK_COMMENT,
        @Query("bookId") bookId: Int,
        @Query("commentId") commentId: Int,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<String>

    /**
     * 加入书架
     */
    @POST()
    suspend fun addToBookShelf(
        @Url url: String = NetProperty.ADD_TO_BOOK_SHELF,
        @Query("session") session: String = GlobalConfig.session,
        @Query("bookId") bookId: Int
    ): RequestRsp<String>

    /**
     * 移出书架
     */
    @POST()
    suspend fun removeFromBookShelf(
        @Url url: String = NetProperty.REMOVE_FROM_BOOK_SHELF,
        @Query("session") session: String = GlobalConfig.session,
        @Query("bookId") bookId: Int
    ): RequestRsp<String>

    /**
     * 添加书籍评论
     */
    @POST()
    suspend fun addBookComment(
        @Url url: String = NetProperty.ADD_BOOK_COMMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Query("bookId") bookId: Int,
        @Query("textContent") textContent: String
    ): RequestRsp<BookComment>

    /**
     * 删除书籍评论
     */
    @POST()
    suspend fun removeBookComment(
        @Url url: String = NetProperty.REMOVE_BOOK_COMMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Query("commentId") commentId: Int
    ): RequestRsp<String>

    /**
     * 获取章节目录
     */
    @GET()
    suspend fun getBookChapterList(
        @Url url: String = NetProperty.GET_BOOK_CHAPTER_LIST,
        @Query("bookId") bookId: Int,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<BookChapterItem>>

    /**
     * 获取单章正文
     * contentHash：本地已有该章时传本地指纹，一致则服务端不下发正文
     */
    @GET()
    suspend fun getBookChapter(
        @Url url: String = NetProperty.GET_BOOK_CHAPTER,
        @Query("bookId") bookId: Int,
        @Query("seq") seq: Int,
        @Query("contentHash") contentHash: String,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<BookChapter>

    /**
     * 批量获取正文
     * 整部离线下载用，一次最多 20 章，含繁体正文
     */
    @GET()
    suspend fun getBookChapters(
        @Url url: String = NetProperty.GET_BOOK_CHAPTERS,
        @Query("bookId") bookId: Int,
        @Query("startSeq") startSeq: Int,
        @Query("endSeq") endSeq: Int,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<BookChapter>>

    /**
     * 获取读经抄经进度
     */
    @GET()
    suspend fun getBookProgress(
        @Url url: String = NetProperty.GET_BOOK_PROGRESS,
        @Query("bookId") bookId: Int,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<BookProgress>

    /**
     * 更新读经进度
     */
    @POST()
    suspend fun updateReadProgress(
        @Url url: String = NetProperty.UPDATE_READ_PROGRESS,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody?
    ): RequestRsp<BookProgress>

    /**
     * 更新抄经进度
     */
    @POST()
    suspend fun updateCopyProgress(
        @Url url: String = NetProperty.UPDATE_COPY_PROGRESS,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody?
    ): RequestRsp<BookProgress>

    /**
     * 获取抄经显示设置
     */
    @GET()
    suspend fun getCopySetting(
        @Url url: String = NetProperty.GET_COPY_SETTING,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<BookCopySetting>

    /**
     * 保存抄经显示设置
     */
    @POST()
    suspend fun updateCopySetting(
        @Url url: String = NetProperty.UPDATE_COPY_SETTING,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody?
    ): RequestRsp<BookCopySetting>

    /**
     * 获取读经显示设置
     */
    @GET()
    suspend fun getReadSetting(
        @Url url: String = NetProperty.GET_READ_SETTING,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<BookReadSetting>

    /**
     * 保存读经显示设置
     */
    @POST()
    suspend fun updateReadSetting(
        @Url url: String = NetProperty.UPDATE_READ_SETTING,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody?
    ): RequestRsp<BookReadSetting>

    /**
     * 获取签到信息：基础奖励、下一档连签奖励、当前连签天数
     */
    @GET()
    suspend fun getSignInData(
        @Url url: String = NetProperty.GET_SIGN_IN_DATA,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<SignInData>

    /**
     * 签到
     */
    @POST()
    suspend fun signIn(
        @Url url: String = NetProperty.SIGN_IN,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<String>

    /**
     * 获取月历：整月格子（公历+农历+是否签到）与今日详情（佛历/干支/宜忌）
     * year、month 不传默认当月，仅限当前时间前后一年
     */
    @GET()
    suspend fun getMonthCalendar(
        @Url url: String = NetProperty.GET_MONTH_CALENDAR,
        @Query("session") session: String = GlobalConfig.session,
        @Query("year") year: Int = 0,
        @Query("month") month: Int = 0
    ): RequestRsp<CalendarSimpleRsp>

    /**
     * 获取许愿文本示例
     */
    @GET()
    suspend fun getWishingTag(
        @Url url: String = NetProperty.GET_WISHING_TAG,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<String>>

    /**
     * 许愿
     */
    @POST()
    suspend fun postWishing(
        @Url url: String = NetProperty.POST_WISHING,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody
    ): RequestRsp<String>

    /**
     * 删除许愿
     */
    @POST()
    suspend fun deleteWishing(
        @Url url: String = NetProperty.DELETE_WISHING,
        @Query("session") session: String = GlobalConfig.session,
        @Query("wishingId") wishingId: Int
    ): RequestRsp<String>

    /**
     * 添加祝福
     */
    @POST()
    suspend fun blessWishing(
        @Url url: String = NetProperty.BLESS_WISHING,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody
    ): RequestRsp<String>

    /**
     * 设置愿望已读，body 是裸 JSON 数组
     */
    @POST()
    suspend fun readWishing(
        @Url url: String = NetProperty.READ_WISHING,
        @Query("session") session: String = GlobalConfig.session,
        @Body request: RequestBody
    ): RequestRsp<String>

    /**
     * 获取许愿树：随机 20 条近 90 天、近 30 天未看过的愿望
     */
    @GET()
    suspend fun getWishingTree(
        @Url url: String = NetProperty.GET_WISHING_TREE,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<WishRecordBean>>

    /**
     * 获取我的许愿记录，pageNum 从 0 开始
     */
    @GET()
    suspend fun getMyWishingRecord(
        @Url url: String = NetProperty.GET_MY_WISHING_RECORD,
        @Query("session") session: String = GlobalConfig.session,
        @Query("pageNum") pageNum: Int
    ): RequestRsp<List<WishRecordBean>>

    /**
     * 获取我的祝福记录，pageNum 从 0 开始
     */
    @GET()
    suspend fun getMyBlessRecord(
        @Url url: String = NetProperty.GET_MY_BLESS_RECORD,
        @Query("session") session: String = GlobalConfig.session,
        @Query("pageNum") pageNum: Int
    ): RequestRsp<List<BlessRecordBean>>

    /**
     * 根据愿望id获取祝福记录，pageNum 从 0 开始
     */
    @GET()
    suspend fun getBlessRecordByWishingId(
        @Url url: String = NetProperty.GET_BLESS_RECORD_BY_WISHING_ID,
        @Query("session") session: String = GlobalConfig.session,
        @Query("wishingId") wishingId: Int,
        @Query("pageNum") pageNum: Int
    ): RequestRsp<List<BlessRecordBean>>

    //region 祈福

    /**
     * 福卡墙，随机20张
     */
    @GET()
    suspend fun getPrayingWall(
        @Url url: String = NetProperty.GET_PRAYING_WALL,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<PrayingBean>>

    /**
     * 所有祈福，pageNum 从 0 开始
     */
    @GET()
    suspend fun getAllPrayingRecord(
        @Url url: String = NetProperty.GET_ALL_PRAYING_RECORD,
        @Query("session") session: String = GlobalConfig.session,
        @Query("pageNum") pageNum: Int
    ): RequestRsp<List<PrayingBean>>

    /**
     * 我的祈福，pageNum 从 0 开始
     */
    @GET()
    suspend fun getMyPrayingRecord(
        @Url url: String = NetProperty.GET_MY_PRAYING_RECORD,
        @Query("session") session: String = GlobalConfig.session,
        @Query("pageNum") pageNum: Int
    ): RequestRsp<List<PrayingBean>>

    /**
     * 发布祈福，内容限20字；coverType>0 为付费封面，消耗5功德币
     */
    @POST()
    suspend fun postPraying(
        @Url url: String = NetProperty.POST_PRAYING,
        @Query("session") session: String = GlobalConfig.session,
        @Query("content") content: String,
        @Query("coverType") coverType: Int = 0
    ): RequestRsp<String>

    /**
     * 删除祈福
     */
    @POST()
    suspend fun deletePraying(
        @Url url: String = NetProperty.DELETE_PRAYING,
        @Query("session") session: String = GlobalConfig.session,
        @Query("prayingId") prayingId: Int
    ): RequestRsp<String>

    /**
     * 祝福福卡，每日5次，每次得5修行值
     */
    @POST()
    suspend fun blessPraying(
        @Url url: String = NetProperty.BLESS_PRAYING,
        @Query("session") session: String = GlobalConfig.session,
        @Query("prayingId") prayingId: Int
    ): RequestRsp<String>

    /**
     * 今日已祝福次数
     */
    @GET()
    suspend fun getPrayingStatus(
        @Url url: String = NetProperty.GET_PRAYING_STATUS,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<Int>

    //endregion

    //region 行善

    /**
     * 善行列表，currPage 从 0 开始；category 传空为全部
     */
    @GET()
    suspend fun getGoodDeedList(
        @Url url: String = NetProperty.GET_GOOD_DEED_LIST,
        @Query("session") session: String = GlobalConfig.session,
        @Query("currPage") currPage: Int,
        @Query("category") category: String? = ""
    ): RequestRsp<List<DeedBean>>

    /**
     * 善行详情（含我的捐赠统计）
     */
    @GET()
    suspend fun getGoodDeedInfo(
        @Url url: String = NetProperty.GET_GOOD_DEED_INFO,
        @Query("session") session: String = GlobalConfig.session,
        @Query("deedId") deedId: Int
    ): RequestRsp<DeedBean>

    /**
     * 发布求助：标题限32字，目标功德币>0
     */
    @POST()
    suspend fun postGoodDeed(
        @Url url: String = NetProperty.POST_GOOD_DEED,
        @Query("session") session: String = GlobalConfig.session,
        @Query("title") title: String,
        @Query("content") content: String,
        @Query("category") category: String? = "",
        @Query("isUrgent") isUrgent: Int = 0,
        @Query("targetCoins") targetCoins: Int
    ): RequestRsp<String>

    /**
     * 删除求助
     */
    @POST()
    suspend fun deleteGoodDeed(
        @Url url: String = NetProperty.DELETE_GOOD_DEED,
        @Query("session") session: String = GlobalConfig.session,
        @Query("deedId") deedId: Int
    ): RequestRsp<String>

    /**
     * 行善捐赠功德币
     */
    @POST()
    suspend fun donateGoodDeed(
        @Url url: String = NetProperty.DONATE_GOOD_DEED,
        @Query("session") session: String = GlobalConfig.session,
        @Query("deedId") deedId: Int,
        @Query("coins") coins: Int,
        @Query("message") message: String? = null
    ): RequestRsp<String>

    /**
     * 分享助力
     */
    @POST()
    suspend fun shareGoodDeed(
        @Url url: String = NetProperty.SHARE_GOOD_DEED,
        @Query("session") session: String = GlobalConfig.session,
        @Query("deedId") deedId: Int
    ): RequestRsp<String>

    /**
     * 善行捐赠记录，pageNum 从 0 开始
     */
    @GET()
    suspend fun getDeedDonateRecord(
        @Url url: String = NetProperty.GET_DEED_DONATE_RECORD,
        @Query("session") session: String = GlobalConfig.session,
        @Query("deedId") deedId: Int,
        @Query("pageNum") pageNum: Int
    ): RequestRsp<List<DeedDonateBean>>

    /**
     * 我发布的求助，pageNum 从 0 开始
     */
    @GET()
    suspend fun getMyDeedRecord(
        @Url url: String = NetProperty.GET_MY_DEED_RECORD,
        @Query("session") session: String = GlobalConfig.session,
        @Query("pageNum") pageNum: Int
    ): RequestRsp<List<DeedBean>>

    /**
     * 我捐赠的善行，pageNum 从 0 开始
     */
    @GET()
    suspend fun getMyDonateDeedRecord(
        @Url url: String = NetProperty.GET_MY_DONATE_DEED_RECORD,
        @Query("session") session: String = GlobalConfig.session,
        @Query("pageNum") pageNum: Int
    ): RequestRsp<List<DeedBean>>

    //endregion

    //region 寺庙

    /**
     * 寺庙列表，currPage 从 0 开始；经纬度可不传（传0）
     */
    @GET()
    suspend fun getTempleList(
        @Url url: String = NetProperty.GET_TEMPLE_LIST,
        @Query("session") session: String = GlobalConfig.session,
        @Query("currPage") currPage: Int,
        @Query("latitude") latitude: Float = 0f,
        @Query("longitude") longitude: Float = 0f
    ): RequestRsp<List<TempleBean>>

    /**
     * 寺庙详情
     */
    @GET()
    suspend fun getTempleInfo(
        @Url url: String = NetProperty.GET_TEMPLE_INFO,
        @Query("session") session: String = GlobalConfig.session,
        @Query("templeId") templeId: Int,
        @Query("latitude") latitude: Float = 0f,
        @Query("longitude") longitude: Float = 0f
    ): RequestRsp<TempleBean>

    /**
     * 提交推荐，recommendStatus 1推荐 2不推荐
     */
    @POST()
    suspend fun postRecommendStatus(
        @Url url: String = NetProperty.POST_RECOMMEND_STATUS,
        @Query("session") session: String = GlobalConfig.session,
        @Query("templeId") templeId: Int,
        @Query("recommendStatus") recommendStatus: Int
    ): RequestRsp<String>

    /**
     * 发表寺庙评论
     */
    @POST()
    suspend fun postTempleComment(
        @Url url: String = NetProperty.POST_TEMPLE_COMMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Query("templeId") templeId: Int,
        @Query("comment") comment: String
    ): RequestRsp<String>

    /**
     * 寺庙评论列表，pageNum 从 0 开始
     */
    @GET()
    suspend fun getTempleComment(
        @Url url: String = NetProperty.GET_TEMPLE_COMMENT,
        @Query("session") session: String = GlobalConfig.session,
        @Query("templeId") templeId: Int,
        @Query("pageNum") pageNum: Int
    ): RequestRsp<List<TempleCommentBean>>

    //endregion

    //region 功德捐赠

    /**
     * 功德佛像列表
     */
    @GET()
    suspend fun getDonateBuddhaList(
        @Url url: String = NetProperty.GET_DONATE_BUDDHA_LIST,
        @Query("session") session: String = GlobalConfig.session
    ): RequestRsp<List<DonateBuddhaBean>>

    /**
     * 功德佛像详情
     */
    @GET()
    suspend fun getDonateBuddhaDetail(
        @Url url: String = NetProperty.GET_DONATE_BUDDHA_DETAIL,
        @Query("session") session: String = GlobalConfig.session,
        @Query("buddhaId") buddhaId: Int
    ): RequestRsp<DonateBuddhaBean>

    /**
     * 功德捐赠，返回最新账户信息
     */
    @POST()
    suspend fun donateBuddha(
        @Url url: String = NetProperty.DONATE_BUDDHA,
        @Query("session") session: String = GlobalConfig.session,
        @Query("buddhaId") buddhaId: Int,
        @Query("coins") coins: Int
    ): RequestRsp<PersonData>

    /**
     * 功德捐赠记录，buddhaId 传0获取所有记录，pageNum 从 0 开始
     */
    @GET()
    suspend fun getDonateRecord(
        @Url url: String = NetProperty.GET_DONATE_RECORD,
        @Query("session") session: String = GlobalConfig.session,
        @Query("buddhaId") buddhaId: Int = 0,
        @Query("pageNum") pageNum: Int
    ): RequestRsp<List<DonateRecordBean>>

    /**
     * 福袋领取
     */
    @POST()
    suspend fun gainLuckyBag(
        @Url url: String = NetProperty.GAIN_LUCKY_BAG,
        @Query("session") session: String = GlobalConfig.session,
        @Query("buddhaId") buddhaId: Int
    ): RequestRsp<LuckyBagBean>

    //endregion

    //region 用户

    /**
     * 获取用户信息
     */
    @GET()
    suspend fun getUserInfo(
        @Url url: String = NetProperty.GET_USER_INFO,
        @Query("session") session: String = GlobalConfig.session,
        @Query("accountId") accountId: Int
    ): RequestRsp<PersonData>

    /**
     * 修改个人资料，传空字符串表示不修改该项
     */
    @POST()
    suspend fun modifyProfile(
        @Url url: String = NetProperty.MODIFY_PROFILE,
        @Query("session") session: String = GlobalConfig.session,
        @Query("userName") userName: String? = null,
        @Query("avatar") avatar: String? = null,
        @Query("mood") mood: String? = null,
        @Query("gender") gender: Int = -1,
        @Query("cover") cover: String? = null
    ): RequestRsp<String>

    /**
     * 关注
     */
    @POST()
    suspend fun follow(
        @Url url: String = NetProperty.FOLLOW,
        @Query("session") session: String = GlobalConfig.session,
        @Query("targetAccountId") targetAccountId: Int
    ): RequestRsp<String>

    /**
     * 取消关注
     */
    @POST()
    suspend fun unFollow(
        @Url url: String = NetProperty.UN_FOLLOW,
        @Query("session") session: String = GlobalConfig.session,
        @Query("targetAccountId") targetAccountId: Int
    ): RequestRsp<String>

    //endregion
}
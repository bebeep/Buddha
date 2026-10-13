package com.fingertip.baselib.net

import com.fingertip.baselib.bean.BookComment
import com.fingertip.baselib.bean.BookThemeEntity
import com.fingertip.baselib.bean.BookTypeConfig
import com.fingertip.baselib.bean.BuddhaBook
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.bean.OssConfig
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.bean.RequestRsp
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
}
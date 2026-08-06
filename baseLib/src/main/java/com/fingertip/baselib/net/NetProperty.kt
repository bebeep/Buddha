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
    var TEST_URL            = "http://192.168.10.195:8888/buddha/"
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

}

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
    var CHECK_SERVER_STATUS             = "config/checkUpdate"


    /**
     * 登录
     */
    var LOGIN                           = "account/login"


    /**
     * 登出
     */
    var LOGOUT                          = "account/logout"


}

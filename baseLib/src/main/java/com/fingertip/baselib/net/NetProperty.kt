package com.fingertip.baselib.net

import com.fingertip.baseLib.BuildConfig


object NetProperty {

    private const val API_PATH = "/api/"



    /**
     * 测试服
     */
    var TEST_URL            = "https://test.livegirl.me:9000"

    /**
     * 正式服
     */
    var ZS_URL              = "https://v.51qifei.xyz:9002"


    var SERVER_URL: String
        get() {
            return  if (BuildConfig.DEBUG) TEST_URL else ZS_URL
        }
        set(value) {}

    /**
     * 公共请求头
     */
    val BASE_URL: String
        get() {
            return SERVER_URL + API_PATH
        }



    /********************** 以下是接口 **********************/



    /**
     * 绑定账号
     * @variable {Account/BindAccount}
     */
    var ACTBIND                           = "Account/BindAccount"

    /**
     * 登录
     * @variable {Account/BindAccount}
     */
    var LOGIN                           = "Account/BindAccount"


    /**
     * 登出
     * @variable {Account/Logout}
     */
    var LOGOUT                           = "Account/Logout"


}

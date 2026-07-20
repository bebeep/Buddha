package com.fingertip.baselib.constant

import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.bean.GlobalParam
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.bean.ServerConfigBean
import com.fingertip.baselib.bean.VersionInfo

/**
 * 用户信息、全局配置相关
 */
object GlobalConfig {



    const val spName = "YHManager"

    // 存放用户登陆信息的sp key
    const val spYH = "spYH"

    // 存放配置信息的sp key
    private const val spParam = "spParam"

    /**
     * 用户登陆信息
     */
    var userData: PersonData? = null
        get() {
            return if (field != null) {
                field
            } else {
                try {
                    val str = SPUtils.getInstance(spName).getString(spYH, "{}")
                    GsonUtils.fromJson(str, PersonData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
        set(value) {
            field = value
            val str = GsonUtils.toJson(field)
            SPUtils.getInstance(spName).put(spYH, str)
        }

    /**
     * 全局参数
     */
    var globalParam: GlobalParam? = null

    //版本更新信息
    var versionInfo: VersionInfo? = null

    /**
     * 登陆的session
     */
    var session: String = ""


    /**
     * OneSignal id
     */
    var PUSH_ID: String? = null

    /**
     * 是否是账号登陆
     */
    var isAccountLogin: Boolean
        get() {
            return SPUtils.getInstance(spName).getBoolean("isAccountLogin",true)
        }
        set(value) {
            SPUtils.getInstance(spName).put("isAccountLogin", value)
        }



    /**
     * tabBar配置
     */
    var tabBarConfig: ServerConfigBean.TabBarConfig? = null

}
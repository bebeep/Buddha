package com.fingertip.baselib.bean

import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.yokeyword.fragmentation.SupportFragment

/**
 * 用户信息、全局配置相关
 */
object YHManager {

    private val TAG = "YHManager"

    private const val spName = "YHManager"

    // 存放用户登陆信息的sp key
    private const val spYH = "spYH"

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
     * 是否是试玩登陆
     */
    var isTryLogin: Boolean
        get() {
            return SPUtils.getInstance(spName).getBoolean("isTryLogin")
        }
        set(value) {
            SPUtils.getInstance(spName).put("isTryLogin", value)
        }




}
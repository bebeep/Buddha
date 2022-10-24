package com.fingertip.baselib.top

import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ScreenUtils
import java.lang.System.currentTimeMillis

object TopUtils {

    private var lastTime_click: Long = 0L
    private const val TIME_INTERVAL = 500L
    /**
     * 是否连续点击
     */
    fun isTwiceClick(): Boolean {
        // 避免连续点击
        if (currentTimeMillis() - lastTime_click < TIME_INTERVAL) {
            lastTime_click = currentTimeMillis()
            return true
        }
        lastTime_click = currentTimeMillis()
        return false
    }
    fun isClickHuntCall(){
        lastTime_click = currentTimeMillis()+2000L
    }
    fun mlclick(){
        lastTime_click = currentTimeMillis()
    }
    fun restore(){
        //lastTime_click =0L
    }

    /**
     * 邮箱验证正则表达式
     */
    val yzEmail = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$"

    val sjInfo=android.os.Build.BRAND+"  "+android.os.Build.MODEL


    var PHONE_WIDTH: Int = 0
        get() {
            if (field == 0) {
                field = ScreenUtils.getScreenWidth()
            }
            return field
        }

    var PHONE_HEIGHT: Int = 0
        get() {
            if (field == 0) {
                field = ScreenUtils.getScreenHeight()
            }
            return field
        }

    fun saveLoginInfo(email: String, pwd: String) {
        SPUtils.getInstance().put("login_email", email)
        SPUtils.getInstance().put("login_pwd", pwd)
    }

    fun getLastLoginEmail(): String {
        return SPUtils.getInstance().getString("login_email", "")
    }

    fun getLastLoginPwd(): String {
        return SPUtils.getInstance().getString("login_pwd", "")
    }
}
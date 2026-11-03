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


    /**
     * 角色类型
     */
     object ROLE_TYPE {
        //普通用户
        const val ROLE_TYPE_NORMAL: Int = 1

        //认证大师
        const val ROLE_TYPE_MASTER: Int = 2
    }

    const val spName = "YHManager"

    // 存放用户登陆信息的sp key
    const val spYH = "spYH"

    // 最近选中的佛像id（拜佛页恢复用）。切佛时本地写入；登录响应带服务端值时同步覆盖
    const val SP_LAST_BUDDHA = "spLastBuddhaId"

    // 存放配置信息的sp key
    private const val spParam = "spParam"

    /**
     * 用户登陆信息
     */
    var userData: PersonData? = null
        get() {
            if (field != null) return field

            // 冷启动时从 SP 恢复。
            // 必须同时把 session 填回去：进程被回收后 GlobalConfig.session 是空的，
            // 而所有接口默认参数都读它，不填就会出现"明明登录着，一进来全部接口 session 无效"。
            field = try {
                val str = SPUtils.getInstance(spName).getString(spYH, "{}")
                GsonUtils.fromJson(str, PersonData::class.java)
            } catch (e: Exception) {
                null
            }
            if (field != null && (field?.accountId ?: 0) <= 0) {
                // SP 里只有 "{}" 时 Gson 会解析出一个空壳对象，当成未登录处理
                field = null
            }
            session = field?.latestSession ?: ""
            return field
        }
        set(value) {
            field = value
            val str = GsonUtils.toJson(field)
            SPUtils.getInstance(spName).put(spYH, str)
            session = field?.latestSession ?: ""
            //服务端记录的最近选中佛像同步到本地 key：换设备登录后拜佛页恢复的是
            //服务端那份选择，而不是这台机器上次（可能是别人）的选择
            field?.lastBuddhaId?.takeIf { it > 0 }?.let {
                SPUtils.getInstance(spName).put(SP_LAST_BUDDHA, it)
            }
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
     * 是否游客账号：与服务端 tryPlay 语义对齐——已登录且 email/googleId/appleId 全空。
     * 用于购买前、设置页的「绑定账号」引导；绑定后任一凭证非空即不再是游客。
     */
    val isGuestAccount: Boolean
        get() {
            val u = userData ?: return false
            if (session.isBlank()) return false
            return u.email.isNullOrBlank() && u.googleId.isNullOrBlank() && u.appleId.isNullOrBlank()
        }



    /**
     * tabBar配置
     */
    var tabBarConfig: ServerConfigBean.TabBarConfig? = null

}
package com.fingertip.baselib.bean

/**
 * 首页聚合数据，对应服务端 home/getSummary 返回的 HomeSummaryData
 * （com.buddha.web.model.apiModel.HomeSummaryData）
 *
 * 首屏的修持进度、供灯状态、禅值余额、连签天数一次拿回，
 * 避免首页并发四五个请求在弱网下互相等待。
 */
class HomeSummaryData {
    //禅值余额
    var meritBalance: Int = 0
    //早课连续天数
    var streakDays: Int = 0
    //今天（按用户时区）早课/晚课是否已完成
    var practiceDoneToday: Boolean = false
    //今天是否诵经（book_reach_record.readDate 落在当天）
    var sutraDone: Boolean = false
    //今天是否抄经（book_reach_record.copyDate 落在当天）
    var copyDone: Boolean = false
    //今天是否已签到（sign_in_record）
    var signDone: Boolean = false
    //供灯状态 0无灯 1燃烧中 2将熄（剩余<=3小时）
    var lampStatus: Int = 0
    //灯剩余小时数
    var lampRemainHours: Int = 0
    //灯具外观名称，默认灯时为空
    var lampName: String? = null
    //首页区块列表（运营配置），为空时前端用默认六区块
    var blocks: List<HomeBlockConfig>? = null

    /**
     * 供灯三个字段单独抽出来，灯条渲染只看这个对象，不用在页面里到处传三个值
     */
    fun lamp(): LampStatusData {
        return LampStatusData(lampStatus, lampRemainHours, lampName)
    }
}

/**
 * 供灯状态（首页灯条用），服务端没有单独接口，字段内嵌在 HomeSummaryData 里
 *
 * 三态：无灯（引导首购）/ 燃烧中（静默小字）/ 将熄（剩余<=3小时，高亮+续燃按钮）
 */
class LampStatusData(
    //0无灯 1燃烧中 2将熄
    var status: Int = 0,
    var remainHours: Int = 0,
    var lampName: String? = null
) {
    companion object {
        const val STATUS_NONE = 0
        const val STATUS_BURNING = 1
        const val STATUS_DYING = 2

        //剩余时长到这个小时数以内算「将熄」，与服务端判定保持一致
        const val DYING_HOURS = 3
    }

    fun isNone() = status == STATUS_NONE

    fun isDying() = status == STATUS_DYING

    /**
     * 灯具名的兜底：服务端只给默认灯时 lampName 为空
     */
    fun displayName(): String = lampName?.takeIf { it.isNotBlank() } ?: "长明灯"
}

/**
 * 首页区块运营配置，对应服务端表 home_block_config
 *
 * blockKey 取值：buddhaCard / practice / lampBar / kingkong / game / momentFeed
 * 顺序由 sortOrder 决定（越小越靠上），isEnabled=0 的区块不展示，
 * 运营改配置不用发版。
 */
class HomeBlockConfig {
    var id: Int = 0
    //区块 key，见类注释
    var blockKey: String? = null
    //区块标题
    var blockTitle: String? = null
    //排序，越小越靠上
    var sortOrder: Int = 0
    //0下线 1上线
    var isEnabled: Int = 0
    //区块配置 json（金刚区槽位/修持槽位映射等），一期前端不解析
    var extraData: String? = null
    //更新时间，服务端格式 yyyy-MM-dd HH:mm:ss，前端只透传不展示
    var updateDate: String? = null

    fun isOnline() = isEnabled == 1
}

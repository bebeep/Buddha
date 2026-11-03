package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.BuddhaFestival
import com.fingertip.baselib.bean.CalendarRemindData
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 佛历订阅提醒。对应 userTool 的 getRemindList/saveRemind/deleteRemind/getFestivalList
 *
 * 规则的增删改都不做本地乐观更新：提醒是"配了就会打扰用户"的东西，
 * 必须服务端确认写成功了界面才变，否则用户以为关了、第二天还在收推送。
 */
class RemindVM : TopVMImp() {

    /** 规则列表 */
    val remindListResult = MutableLiveData<RequestResult<List<CalendarRemindData>>>()

    /** 保存结果：成功时把最新列表一并刷一遍 */
    val saveResult = MutableLiveData<RequestResult<String>>()

    /** 删除结果 */
    val deleteResult = MutableLiveData<RequestResult<String>>()

    /** 节日预告 */
    val festivalResult = MutableLiveData<RequestResult<List<BuddhaFestival>>>()

    fun loadRemindList() {
        call({
            NetManager.getApi().getRemindList()
        }, {
            remindListResult.value = successResult(it)
        }, {
            remindListResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 保存规则
     */
    fun saveRemind(rule: CalendarRemindData) {
        call({
            NetManager.getApi().saveRemind(
                body = RequestBodyFactory.remindBody(
                    id = rule.id,
                    remindType = rule.remindType,
                    festivalKey = rule.festivalKey.orEmpty(),
                    remindOffset = rule.remindOffset,
                    hour = rule.hour(),
                    enabled = rule.isEnabled
                )
            )
        }, {
            saveResult.value = successResult(it)
        }, {
            saveResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 只切开关的便捷方法：其余字段原样带回去，避免把提前天数/时刻冲掉
     */
    fun switchRemind(rule: CalendarRemindData, enabled: Boolean) {
        val copy = CalendarRemindData()
        copy.id = rule.id
        copy.remindType = rule.remindType
        copy.festivalKey = rule.festivalKey
        copy.remindOffset = rule.remindOffset
        copy.remindTime = rule.remindTime
        copy.remindTimeText = rule.remindTimeText
        copy.isEnabled = if (enabled) 1 else 0
        saveRemind(copy)
    }

    fun deleteRemind(remindId: Int) {
        call({
            NetManager.getApi().deleteRemind(remindId = remindId)
        }, {
            deleteResult.value = successResult(it)
        }, {
            deleteResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }

    /**
     * 未来一年内的佛历节日，页面上做"接下来会提醒你什么"的预告。
     * 只取前 90 天：再往后用户也不会真的去核对日期。
     */
    fun loadFestivals(days: Int = 90) {
        call({
            NetManager.getApi().getFestivalList(days = days)
        }, {
            festivalResult.value = successResult(it)
        }, {
            festivalResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = false, toastError = false)
    }
}

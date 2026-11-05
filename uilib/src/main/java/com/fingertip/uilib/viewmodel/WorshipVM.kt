package com.fingertip.uilib.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.constant.GlobalConfig
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 拜佛页面 ViewModel
 *
 * 多佛像：buddha_config 里每尊佛像独立配置，页面通过 [selectBuddha] 切换当前佛像。
 * 场景为 2D 分层展示（静态图 + 预留 Lottie 动态场景层），无 3D 模型下载——
 * 佛像展示图直接用配置里的 buddhaCoverUrl（兜底 buddhaPicture），Glide 自带磁盘缓存。
 *
 * 负责：
 * - 佛像列表初始化与选中恢复（本地 SP > 第一尊）
 * - 选中持久化：本地 SP 即时落盘 + 登录后 saveLastBuddha 异步上报
 * - 供佛（按价目选天数，服务端扣功德币）
 */
class WorshipVM : TopVMImp() {

    companion object {
        /** 本地记录的最近选中佛像，0=从未选过。key 在 GlobalConfig：登录时 userData 落盘要把服务端值同步进来 */
        private val SP_LAST_BUDDHA = GlobalConfig.SP_LAST_BUDDHA

        /** 本地记录的最近选中佛像，0=从未选过 */
        fun savedBuddhaId(): Int =
            SPUtils.getInstance(GlobalConfig.spName).getInt(SP_LAST_BUDDHA, 0)

        /**
         * 解析佛像供奉价目 priceField，如 [{"days":2,"price":98},{"days":8,"price":298}]
         */
        fun parsePriceField(priceField: String?): List<PriceItem> {
            if (priceField.isNullOrBlank()) return emptyList()
            return try {
                GsonUtils.fromJson(priceField, GsonUtils.getListType(PriceItem::class.java)) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    val buddhaConfigResult = MutableLiveData<RequestResult<List<BuddhaConfig>>>()

    /** 佛像列表（globalParam 下发优先，空则调接口刷新） */
    private var buddhaList: List<BuddhaConfig> = emptyList()

    /** 当前选中的佛像，null=列表还没就绪 */
    private val _selectedBuddha = MutableLiveData<BuddhaConfig?>()
    val selectedBuddha: LiveData<BuddhaConfig?> = _selectedBuddha

    /**
     * 初始化佛像列表，并恢复上次选中的佛像。
     * 恢复优先级：本地 SP 记录（含登录时从服务端同步来的值）> 第一尊。
     * globalParam 里已有列表就先渲染首屏（启动时下发，游客也能看）；
     * 但 globalParam 只有静态配置——供佛状态/香烛贡品档次/灯具素材这些
     * 按账号的数据只有 getBuddhaConfig 接口才有，所以登录态必须再调一次
     * 接口覆盖（未登录接口也无数据，globalParam 为空时才作为兜底去调）。
     */
    fun initBuddhaList() {
        val fromParam = GlobalConfig.globalParam?.buddhaConfig.orEmpty()
        if (fromParam.isNotEmpty()) {
            buddhaList = fromParam
            restoreLast(fromParam)
        }
        if (GlobalConfig.session.isNotBlank() || fromParam.isEmpty()) {
            getBuddhaConfig()
        }
    }

    /** 按 SP 记录恢复；记录的佛像已不在列表里（下架/换设备测试数据）就回退第一尊 */
    private fun restoreLast(list: List<BuddhaConfig>) {
        val saved = savedBuddhaId()
        val target = list.firstOrNull { it.id == saved } ?: list.first()
        selectBuddha(target)
    }

    /**
     * 接口兜底回来的列表（globalParam 拉取失败的场景），同样走恢复逻辑
     */
    fun restoreOrFirst(list: List<BuddhaConfig>) {
        buddhaList = list
        restoreLast(list)
    }

    /**
     * 切换当前佛像，同时把选择持久化（本地 + 服务端）。
     * 已是同一尊就不重复通知（防连点重复动画）。
     */
    fun selectBuddha(buddha: BuddhaConfig) {
        val same = _selectedBuddha.value?.id == buddha.id
        _selectedBuddha.value = buddha
        //本地立即落盘：进程被杀也能恢复到这次的选择
        SPUtils.getInstance(GlobalConfig.spName).put(SP_LAST_BUDDHA, buddha.id)
        //服务端异步上报：失败静默，下次切换会再报；没登录就不报（游客选择只存本地）
        if (GlobalConfig.session.isNotBlank() && !same) {
            call({
                NetManager.getApi().saveLastBuddha(buddhaId = buddha.id)
            }, {}, {}, showLoading = false, toastError = false)
        }
    }

    fun getBuddhaConfig() {
        call({
            NetManager.getApi().getBuddhaConfig()
        }, {
            buddhaConfigResult.value = successResult(it)
        }, {
            buddhaConfigResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    /** 供佛结果：成功后返回新的供奉剩余秒数（服务端不回，由页面重拉列表） */
    val worshipResult = MutableLiveData<RequestResult<String>>()

    /**
     * 供佛：按价目选择供奉天数，服务端扣功德币；已在供则顺延
     */
    fun worshipBuddha(buddhaId: Int, days: Int) {
        call({
            NetManager.getApi().worshipBuddha(
                body = RequestBodyFactory.worshipBody(buddhaId, days)
            )
        }, {
            worshipResult.value = successResult(it)
        }, {
            worshipResult.value = failResult(it.errorCode)
        })
    }

    /** 供奉价目项（对应服务端 PriceFieldConfig） */
    data class PriceItem(val days: Int = 0, val price: Int = 0)
}

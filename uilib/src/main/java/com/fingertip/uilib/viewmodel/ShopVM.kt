package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.MyDressData
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 装扮商城。对应服务端 dress 一组接口。
 *
 * 购买统一走功德币（全站唯一货币）：buyByCoin 一步完成「扣币 + 发货」，
 * 成功返回扣费后的余额，页面直接回写 GlobalConfig.userData.coin 即可。
 * 不再有「建单 → 商店支付 → 验单」三步——那是 IAP 时代的链路。
 *
 * 所有写操作都带 showLoading，用户点了要有反馈；
 * 错误不弹 toast，由页面决定怎么提示（避免 VM 弹一次、页面再弹一次）。
 */
class ShopVM : TopVMImp() {

    //当前分类的商品列表
    val listResult = MutableLiveData<RequestResult<List<ShopDressConfig>>>()

    //我持有的装扮 + 佩戴状态
    val myResult = MutableLiveData<RequestResult<MyDressData>>()

    //功德币购买结果：data 是扣费后的功德币余额
    val buyResult = MutableLiveData<RequestResult<String>>()

    //佩戴/摘下结果，data 是服务端原样返回的字符串
    val wearResult = MutableLiveData<RequestResult<String>>()

    //防止分类快速切换时旧请求覆盖新分类的数据
    private var currentType = 0

    /**
     * 拉商品列表。游客也能拉（只能逛不能买）
     * @param dressType 1灯具 2佛龛 3头像框 4昵称3D 5灯油
     */
    fun loadList(dressType: Int, showLoading: Boolean = false) {
        currentType = dressType
        call({
            NetManager.getApi().getDressList(dressType = dressType)
        }, {
            //回来的不是当前分类的请求就丢掉（用户已经切到别的 tab）
            if (currentType == dressType) {
                listResult.value = successResult(it)
            }
        }, {
            if (currentType == dressType) {
                listResult.value = failResult(it.errorCode, it.errorMsg)
            }
        }, showLoading = showLoading, toastError = false)
    }

    /**
     * 我的装扮。未登录时服务端回 INVALID_SESSION，页面静默忽略即可
     */
    fun loadMyDress(showLoading: Boolean = false) {
        call({
            NetManager.getApi().getMyDress()
        }, {
            myResult.value = successResult(it)
        }, {
            myResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = showLoading, toastError = false)
    }

    /**
     * 功德币购买。服务端扣币并发货，返回扣费后的余额。
     * @param buddhaId 买灯油时目标佛像 id，其它品类传 0
     */
    fun buyByCoin(dressId: Int, buddhaId: Int = 0) {
        call({
            NetManager.getApi().buyDressByCoin(dressId = dressId, buddhaId = buddhaId)
        }, {
            buyResult.value = successResult(it)
        }, {
            buyResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 佩戴/摘下
     */
    fun wear(dressId: Int, wear: Boolean) {
        call({
            NetManager.getApi().wearDress(body = RequestBodyFactory.dressWearBody(dressId, wear))
        }, {
            wearResult.value = successResult(it)
        }, {
            wearResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }
}

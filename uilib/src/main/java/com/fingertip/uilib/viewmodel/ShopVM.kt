package com.fingertip.uilib.viewmodel

import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.MyDressData
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp

/**
 * 装扮商城。对应服务端 dress 一组接口 + merit/getBalance。
 *
 * 购买链路拆成两个方法，中间那步「拉起商店支付」由页面调 IapBridge 完成
 * （支付需要 Activity，不放进 VM）：
 *   createOrder(dressId) → 页面拉起支付 → verify(orderNo, payOrderId)
 *
 * 所有写操作（建单/验单/佩戴）都带 showLoading，用户点了要有反馈；
 * 错误不弹 toast，由页面决定怎么提示（避免 VM 弹一次、页面再弹一次）。
 */
class ShopVM : TopVMImp() {

    //当前分类的商品列表
    val listResult = MutableLiveData<RequestResult<List<ShopDressConfig>>>()

    //我持有的装扮 + 佩戴状态
    val myResult = MutableLiveData<RequestResult<MyDressData>>()

    //建单成功，data 是服务端返回的内部订单号
    val orderResult = MutableLiveData<RequestResult<String>>()

    //验单发货结果
    val verifyResult = MutableLiveData<RequestResult<String>>()

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
     * 建内部订单。成功后就该立刻拉起商店支付。
     */
    fun createOrder(dressId: Int, buddhaId: Int = 0) {
        call({
            NetManager.getApi().createDressOrder(dressId = dressId, buddhaId = buddhaId)
        }, {
            orderResult.value = successResult(it)
        }, {
            orderResult.value = failResult(it.errorCode, it.errorMsg)
        }, showLoading = true, toastError = false)
    }

    /**
     * 验单并发货。服务端幂等：重复调用只会返回成功，不会重复发货。
     */
    fun verify(orderNo: String, payOrderId: String) {
        call({
            NetManager.getApi().verifyIapOrder(
                body = RequestBodyFactory.iapVerifyBody(orderNo, com.fingertip.baselib.net.NetProperty.PLATFORM, payOrderId)
            )
        }, {
            verifyResult.value = successResult(it)
        }, {
            verifyResult.value = failResult(it.errorCode, it.errorMsg)
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

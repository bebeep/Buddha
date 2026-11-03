package com.fingertip.uilib.pay

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.fingertip.baselib.log

/**
 * Google Play Billing 封装（Android 端真实支付）
 *
 * 三件容易踩的事，这里都处理了：
 *
 * 1. **必须 acknowledge**：购买后 3 天内不 acknowledge，Google 会自动退款。
 *    永久商品 acknowledge 一次即可；消耗型商品走 consume（consume 本身包含 acknowledgement）。
 *
 * 2. **消耗型 vs 永久型**：
 *    灯油是消耗品，必须 consume 才能再次购买；灯具/佛龛是永久商品，不能 consume
 *    ——一旦 consume，用户就能重复买同一件永久装扮（服务端虽然有拦截，但体验是"付了钱被拒"）。
 *
 * 3. **购买回调是全局的**：PurchasesUpdatedListener 在 BillingClient 上只注册一次，
 *    所以同一时刻只允许一个购买流程在跑，用 pendingCallback 单槽位兜住。
 */
object BillingManager {

    private const val TAG = "BillingManager"

    private var billingClient: BillingClient? = null
    private var initialized = false

    /** 当前这笔购买的回调。购买流程串行，不需要 map */
    private var pendingCallback: ((IapBridge.Result) -> Unit)? = null
    private var pendingConsumable = false

    /** 连接中的等待队列：BillingClient 连接是异步的，多个调用方要排队等结果 */
    private val connectWaiters = mutableListOf<(Boolean) -> Unit>()
    private var connecting = false

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        val callback = pendingCallback
        pendingCallback = null

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            val msg = when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.USER_CANCELED -> "已取消支付"
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "你已拥有该商品"
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "支付服务暂不可用，请稍后再试"
                else -> "支付未完成（${billingResult.responseCode}）"
            }
            log(TAG, "购买未成功: $msg / ${billingResult.debugMessage}")
            callback?.invoke(IapBridge.Result(false, null, msg))
            return@PurchasesUpdatedListener
        }

        val purchase = purchases?.firstOrNull()
        if (purchase == null) {
            callback?.invoke(IapBridge.Result(false, null, "支付结果为空"))
            return@PurchasesUpdatedListener
        }

        // PENDING 是「延迟付款」状态（便利店现金支付等），此时不能发货，
        // 要等服务端后续轮询到 PURCHASED 再发。这里明确告诉用户，别让他干等。
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            callback?.invoke(IapBridge.Result(false, null, "支付处理中，完成后会自动到账"))
            return@PurchasesUpdatedListener
        }

        handlePurchase(purchase, pendingConsumable, callback)
    }

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val client = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        billingClient = client
        connect(client) { }
    }

    /**
     * 拉起购买。
     *
     * @param obfuscatedAccountId 账号标识（Google 建议传混淆值），用于对账时定位到具体用户
     * @param consumable true=消耗型（灯油），买完 consume 才能再买
     */
    fun purchase(
        activity: Activity,
        productId: String,
        obfuscatedAccountId: String,
        consumable: Boolean,
        callback: (IapBridge.Result) -> Unit
    ) {
        if (productId.isBlank()) {
            callback(IapBridge.Result(false, null, "该商品暂不可购买"))
            return
        }
        if (pendingCallback != null) {
            callback(IapBridge.Result(false, null, "上一笔支付还在处理中，请稍候"))
            return
        }

        val client = billingClient ?: BillingClient.newBuilder(activity.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
            .also {
                billingClient = it
                connect(it) { }
            }

        pendingCallback = callback
        pendingConsumable = consumable

        connect(client) { ok ->
            if (!ok) {
                pendingCallback = null
                callback(IapBridge.Result(false, null, "无法连接 Google Play，请检查网络与商店账号"))
                return@connect
            }

            val query = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

            client.queryProductDetailsAsync(query) { result, detailsList ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK || detailsList.isNullOrEmpty()) {
                    // 商品查不到最常见的原因是 Play Console 里没上架 / 没激活 / 包名对不上
                    log(TAG, "查询商品失败 productId=$productId code=${result.responseCode} ${result.debugMessage}")
                    pendingCallback = null
                    callback(IapBridge.Result(false, null, "商品信息获取失败，请稍后再试"))
                    return@queryProductDetailsAsync
                }

                val details: ProductDetails = detailsList.first()
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(details)
                                .build()
                        )
                    )
                    .setObfuscatedAccountId(obfuscatedAccountId)
                    .build()

                activity.runOnUiThread {
                    val launch = client.launchBillingFlow(activity, flowParams)
                    if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
                        log(TAG, "拉起支付失败 code=${launch.responseCode} ${launch.debugMessage}")
                        pendingCallback = null
                        callback(IapBridge.Result(false, null, "无法拉起支付（${launch.responseCode}）"))
                    }
                }
            }
        }
    }

    /**
     * 恢复购买：查询本 Google 账号已拥有且未消耗的商品，返回 purchaseToken 列表。
     */
    fun queryPurchases(callback: (List<String>) -> Unit) {
        val client = billingClient
        if (client == null) {
            callback(emptyList())
            return
        }
        connect(client) { ok ->
            if (!ok) {
                callback(emptyList())
                return@connect
            }
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            client.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    log(TAG, "恢复购买查询失败 code=${result.responseCode}")
                    callback(emptyList())
                    return@queryPurchasesAsync
                }
                val tokens = purchases
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .map { it.purchaseToken }
                callback(tokens)
            }
        }
    }

    /* ==================== 内部 ==================== */

    private fun handlePurchase(
        purchase: Purchase,
        consumable: Boolean,
        callback: ((IapBridge.Result) -> Unit)?
    ) {
        val client = billingClient
        val token = purchase.purchaseToken
        log(TAG, "购买成功 orderId=${purchase.orderId} consumable=$consumable")

        if (consumable) {
            // 先 consume 再回调：消耗型商品不 consume 会导致用户下次无法购买同一件
            val params = ConsumeParams.newBuilder().setPurchaseToken(token).build()
            client?.consumeAsync(params) { consumeResult, _ ->
                if (consumeResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    // consume 失败不影响本次发货，但要让日志留痕，方便排查"第二次买不了"的问题
                    log(TAG, "consume 失败 token=$token code=${consumeResult.responseCode}")
                }
                callback?.invoke(IapBridge.Result(true, token, null))
            }
        } else {
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(token)
                    .build()
                client?.acknowledgePurchase(params) { ackResult ->
                    if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        // 不 acknowledge 的话，3 天后 Google 会自动退款并把商品收回
                        log(TAG, "acknowledge 失败 token=$token code=${ackResult.responseCode}")
                    }
                }
            }
            callback?.invoke(IapBridge.Result(true, token, null))
        }
    }

    private fun connect(client: BillingClient, onReady: (Boolean) -> Unit) {
        if (client.isReady) {
            onReady(true)
            return
        }
        connectWaiters.add(onReady)
        if (connecting) return
        connecting = true

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                if (!ok) {
                    log(TAG, "Billing 连接失败 code=${result.responseCode} ${result.debugMessage}")
                }
                val waiters = connectWaiters.toList()
                connectWaiters.clear()
                waiters.forEach { it(ok) }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                log(TAG, "Billing 连接断开，下次调用会重连")
            }
        })
    }
}

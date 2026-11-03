package com.fingertip.uilib.pay

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.fingertip.baselib.BuildConfig
import com.fingertip.baselib.log
import java.security.MessageDigest

/**
 * IAP 购买桥接：只负责「拉起商店支付并拿回平台交易号」这一步。
 *
 * 完整链路是三段，本类只占中间一段：
 *   1. 服务端 dress/createOrder 建内部订单，拿到 orderNo（有价格和商品快照）
 *   2. 本类拉起 Google Play Billing / StoreKit，用户付款，拿回平台交易号
 *   3. 服务端 dress/verify 用 orderNo + 交易号验单并发货
 *
 * 为什么要先建单再付款：订单号由服务端生成，验单时服务端才能对上「谁买什么、多少钱」。
 * 反过来先付款再建单，一旦客户端崩溃就变成「钱扣了没发货」的客诉。
 *
 * debug 包默认走模拟回执（mock_ 前缀），让建单→发货整条链路在没有 Play 账号时也能联调；
 * 服务端 iap.allow-mock=false 时会直接拒绝这个前缀。
 * 要联调真实支付：把 [isMockMode] 改成 false，并在 Play Console 配内测账号。
 */
object IapBridge {

    private const val TAG = "IapBridge"

    //模拟支付回执的前缀。服务端验单上线后应拒绝这个前缀的订单，防止伪造
    const val MOCK_PREFIX = "mock_"

    //模拟支付的回调延迟，给「正在拉起支付」的 UI 一点可见时间
    private const val MOCK_DELAY_MS = 400L

    data class Result(
        val success: Boolean,
        //平台交易号：Google purchaseToken / Apple originalTransactionId
        val payOrderId: String? = null,
        val message: String? = null
    )

    /** 当前是否模拟支付（仅 debug 包），联调真实支付时改成 false */
    fun isMockMode(): Boolean = BuildConfig.DEBUG

    /**
     * 拉起商店支付。
     *
     * @param productId 商店后台配置的商品 id，见 [com.fingertip.baselib.bean.ShopDressConfig.androidProductId]
     * @param accountId 当前账号 id，会以混淆值随订单一起提交给 Google，便于对账
     * @param consumable true=消耗型（灯油，可重复购买）
     */
    fun launchPurchase(
        activity: Activity?,
        productId: String,
        accountId: Int = 0,
        consumable: Boolean = false,
        onResult: (Result) -> Unit
    ) {
        val main = Handler(Looper.getMainLooper())

        if (productId.isBlank()) {
            //商品没配 iapProductId，属于后台配置缺漏，直接报错比让用户点了没反应好
            main.post { onResult(Result(false, null, "该商品暂不可购买")) }
            return
        }

        if (!isMockMode()) {
            if (activity == null) {
                main.post { onResult(Result(false, null, "页面已关闭，请重试")) }
                return
            }
            BillingManager.purchase(
                activity = activity,
                productId = productId,
                obfuscatedAccountId = obfuscate(accountId),
                consumable = consumable
            ) { result ->
                main.post { onResult(result) }
            }
            return
        }

        //模拟支付：生成一个带时间戳的假交易号
        val mockToken = MOCK_PREFIX + productId + "_" + System.currentTimeMillis()
        log(TAG, "模拟支付 productId=$productId token=$mockToken")
        //记到本地，模拟"平台侧能查到的历史交易"，让恢复购买在 debug 包也能走通
        activity?.applicationContext?.let { saveMockPurchase(it, mockToken) }
        main.postDelayed({
            onResult(Result(true, mockToken, null))
        }, MOCK_DELAY_MS)
    }

    /**
     * 恢复购买：把本账号在平台上买过的交易号取回来，交给服务端补发。
     *
     * 必须在"换设备 / 重装 / 清数据"后仍然有效，所以数据源必须是平台侧，
     * 本地记录只是 debug 包的替身。
     */
    fun restorePurchases(activity: Activity?, onResult: (List<String>) -> Unit) {
        val main = Handler(Looper.getMainLooper())

        if (!isMockMode()) {
            BillingManager.queryPurchases { tokens ->
                log(TAG, "恢复购买，平台侧交易号 ${tokens.size} 条")
                main.post { onResult(tokens) }
            }
            return
        }

        val ctx = activity?.applicationContext
        if (ctx == null) {
            main.post { onResult(emptyList()) }
            return
        }
        val ids = loadMockPurchases(ctx)
        log(TAG, "模拟恢复购买，本地交易号 ${ids.size} 条")
        main.post { onResult(ids) }
    }

    /**
     * Google 建议 obfuscatedAccountId 用不可逆的混淆值，不要直接传数据库主键。
     * 这里做一次加盐 MD5，长度控制在 64 以内。
     */
    private fun obfuscate(accountId: Int): String {
        if (accountId <= 0) return ""
        return try {
            val raw = "buddha#$accountId"
            val md = MessageDigest.getInstance("MD5")
            md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            accountId.toString()
        }
    }

    //region debug 包用的本地交易号记录

    private const val SP_NAME = "iap_mock_store"
    private const val KEY_TOKENS = "tokens"
    private const val MAX_TOKENS = 100

    private fun saveMockPurchase(context: Context, token: String) {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY_TOKENS, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(token)
        //只留最近 100 条，避免这个调试用的集合无限增长
        val trimmed = if (set.size > MAX_TOKENS) set.toList().takeLast(MAX_TOKENS).toSet() else set
        sp.edit().putStringSet(KEY_TOKENS, trimmed).apply()
    }

    private fun loadMockPurchases(context: Context): List<String> {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        return sp.getStringSet(KEY_TOKENS, emptySet())?.toList() ?: emptyList()
    }

    //endregion
}

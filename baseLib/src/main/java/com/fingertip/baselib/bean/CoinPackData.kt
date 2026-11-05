package com.fingertip.baselib.bean

/**
 * 功德币充值档位，对应接口 recharge/getConfig（服务端 wx_pay_config）
 *
 * 比例：price 单位为分，baseCount 基础功德币 + extraCount 赠送币。
 * 当前基础价 1 元 = 10 功德币（600 分 → 60 + 5 币）。
 */
class CoinPackData {
    //档位 id，下单时回传
    var id: Int = 0
    //后台配置项标识
    var productId: String? = null
    //IAP 商品 id，格式 android:xxx,ios:xxx（以 : 分隔，取安卓端）
    var iapProductId: String? = null
    //售价（分）
    var price: Int = 0
    //原价（分），0 表示不打折
    var oriPrice: Int = 0
    //基础功德币
    var baseCount: Int = 0
    //赠送功德币
    var extraCount: Int = 0
    //到账合计
    var totalCoin: Int = 0

    /** 售价文案：¥6.00（分转元） */
    fun priceText(): String = String.format("¥%.2f", price / 100.0)

    /** 到账文案：65 功德币（含赠送 5） */
    fun coinText(): String =
        if (extraCount > 0) "$totalCoin 功德币（含赠送 $extraCount）" else "$totalCoin 功德币"

    /** 安卓端 IAP 商品 id */
    fun androidProductId(): String {
        val raw = iapProductId?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        raw.split(",").forEach { part ->
            val p = part.trim()
            when {
                p.startsWith("android:", true) -> return p.substringAfter(":").trim()
                !p.contains(":") -> return p
            }
        }
        return ""
    }
}

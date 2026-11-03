package com.fingertip.baselib.bean

/**
 * 装扮商城相关数据，对应服务端 dress 一组接口
 * （com.buddha.web.controller.DressController）
 *
 * 品类约定见 ShopDressConfig.TYPE_*；灯油是消耗品，其余四类永久持有。
 */

/**
 * 商品配置，对应表 shop_dress_config，接口 dress/getList
 */
class ShopDressConfig {
    var id: Int = 0
    //1灯具外观 2佛龛场景 3头像框 4昵称3D效果 5灯油(消耗品)
    var dressType: Int = 0
    var dressName: String? = null
    var dressDesc: String? = null
    //预览图，一期没有 CDN 时为空，页面用本地占位块渲染
    var previewImage: String? = null
    //资源标识：3D 类=glb key，头像框=drawable 名，昵称3D=样式模板 key
    var resKey: String? = null
    //商店后台的商品 id，格式 android:xxx,ios:xxx
    var iapProductId: String? = null
    //展示价格（美分）。仅用于展示与订单快照，真实扣款以商店价格为准
    var priceCents: Int = 0
    //灯油有效天数；装扮类 0 = 永久
    var durationDays: Int = 0
    var isOnSale: Int = 0
    var sortOrder: Int = 0
    var updateDate: String? = null

    fun isOil() = dressType == TYPE_OIL

    /** 永久买断类（可佩戴），灯油不是 */
    fun isPermanent() = dressType in TYPE_LAMP..TYPE_NAME3D

    /** 是否已拥有（仅在包里查得到时才为 true，由页面传入的持有集合决定） */
    fun priceText(): String = String.format("$%.2f", priceCents / 100.0)

    /** 时长文案：灯油显示「7 天」，永久显示「永久」 */
    fun durationText(): String = if (isOil()) "$durationDays 天" else "永久"

    /**
     * 取安卓端商品 id。后台格式约定 android:xxx,ios:xxx，
     * 也兼容只填一个裸 id（两端共用）的情况。
     */
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

    companion object {
        const val TYPE_LAMP = 1
        const val TYPE_SHRINE = 2
        const val TYPE_FRAME = 3
        const val TYPE_NAME3D = 4
        const val TYPE_OIL = 5

        /** 分类 tab 文案，下标与品类一致（0 位占位不用） */
        val TYPE_NAMES = arrayOf("", "灯具", "佛龛", "头像框", "昵称 3D", "灯油")
    }
}

/**
 * 我的装扮，接口 dress/getMy：持有列表 + 当前佩戴状态
 */
class MyDressData {
    var dresses: List<MyDressItem>? = null
    var wearing: UserDressWearing? = null

    /** 是否持有某个装扮 */
    fun owns(dressId: Int) = dresses.orEmpty().any { it.dressId == dressId }

    /** 某类是否已佩戴（wearValue 与 dressId 相同） */
    fun isWearing(item: MyDressItem): Boolean = wearing?.idOf(item.dressType) == item.dressId

    /** 该类当前佩戴的装扮 id，0 表示未佩戴 */
    fun wearingId(dressType: Int) = wearing?.idOf(dressType) ?: 0
}

/**
 * 我的装扮单项：持有记录 join 商品配置
 */
class MyDressItem {
    var id: Int = 0
    var accountId: Int = 0
    var dressId: Int = 0
    //来源 1 IAP购买 2 早课里程碑奖励 3 运营赠送
    var source: Int = 0
    var iapOrderId: String? = null
    var isDel: Int = 0
    var createDate: String? = null
    //以下为 join 出来的商品字段
    var dressType: Int = 0
    var dressName: String? = null
    var dressDesc: String? = null
    var previewImage: String? = null
    var resKey: String? = null
    var priceCents: Int = 0
    var durationDays: Int = 0

    fun isDefault() = dressId == 0
}

/**
 * 用户佩戴状态，对应表 user_dress_wearing（一人一套，0 = 默认/未佩戴）
 */
class UserDressWearing {
    var id: Int = 0
    var accountId: Int = 0
    //佩戴灯具，0=默认
    var lampDressId: Int = 0
    //佩戴佛龛场景，0=默认
    var shrineDressId: Int = 0
    //佩戴头像框，0=无
    var frameDressId: Int = 0
    //佩戴昵称3D，0=无
    var name3dDressId: Int = 0
    var updateDate: String? = null

    /**
     * 按品类取当前佩戴的装扮 id，服务端三个字段各管一类。
     * 灯油没有佩戴位，返回 0。
     */
    fun idOf(dressType: Int): Int = when (dressType) {
        ShopDressConfig.TYPE_LAMP -> lampDressId
        ShopDressConfig.TYPE_SHRINE -> shrineDressId
        ShopDressConfig.TYPE_FRAME -> frameDressId
        ShopDressConfig.TYPE_NAME3D -> name3dDressId
        else -> 0
    }
}

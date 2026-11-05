package com.fingertip.baselib.bean

class BuddhaConfig: TopData() {
    var id: Int = 0

    //"佛像名"
    var buddhaName: String? = ""

    //"佛像图册"
    var buddhaPicture: String? = ""

    //"佛像诞辰"
    var buddhaBirthday: String? = ""

    //"佛像简介"
    var buddhaIntro: String? = ""

    //"佛像封面"
    var buddhaCoverUrl: String? = ""

    //"普通模型文件"
    var buddhaModuleUrl: String? = ""

    //"3D模型文件"
    var buddha3DModuleUrl: String? = ""

    //"3D模型文件统一命名"
    var buddha3DModuleName: String? = ""

    //"3D模型文件更新时间戳"
    var buddha3DModuleUpdateDate: String? = ""

    //"动态场景 Lottie JSON 地址（香烟/光晕/火焰，2D 分层渲染），素材未配时为空"
    var sceneAnimUrl: String? = ""

    //"前置动画 Lottie JSON 地址（净瓶/仙草等，渲染于佛像图之上），未配时为空"
    var sceneFrontAnimUrl: String? = ""

    //"默认佛龛装扮id(shop_dress_config.id)，0=无"
    var shrineDressId: Int = 0

    //"默认佛龛场景大图(1080x2400透明PNG)，被用户佩戴的佛龛覆盖"
    var shrineResUrl: String? = ""

    //"默认佛龛氛围动画 Lottie JSON，可空"
    var shrineAnimUrl: String? = ""

    //"供佛生效的香烛档次1~3，0=未供佛"
    var incenseLevel: Int = 0

    //"供佛生效的贡品档次1~3，0=未供佛"
    var offeringLevel: Int = 0

    //"香烛素材大图：三根香杆（随供佛剩余时长缩放，未供佛为空）"
    var incenseResUrl: String? = ""

    //"香炉素材大图（居中固定层，盖住香脚，不随香缩放；未供佛为空）"
    var incenseBurnerResUrl: String? = ""

    //"香烛烟雾动画 Lottie JSON，可空"
    var incenseAnimUrl: String? = ""

    //"贡品素材大图（按生效档次由服务端拼好，未供佛为空）"
    var offeringResUrl: String? = ""

    //"本次供佛总时长(秒)，与 worshipLeftTime 相除得香烛燃烧比例"
    var worshipTotalSec: Long = 0

    //"佛像标签（庇佑）"
    var buddhaTag: List<String?>? = null

    //"推荐佛经"
    var recommendBooks: List<BuddhaBook?>? = null

    //"介绍视频"
    var buddhaVideos: List<BuddhaVideo?>? = null

    //"是否正在供奉中"
    var isWorshipping = false

    //"供奉剩余时间(秒)"
    var worshipLeftTime: Long = 0

    //"供奉价目表"
    var priceField: String? = ""

    //"已供奉人数"
    var worshipCount: Int = 0

    //"拥有人数"
    var ownCount: Int = 0

}
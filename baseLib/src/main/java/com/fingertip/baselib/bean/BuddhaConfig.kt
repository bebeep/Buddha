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

}
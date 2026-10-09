package com.fingertip.baselib.bean

class BookThemeEntity: TopData() {
    var id: Int = 0
    var themeTitle: String? = "" //主题名称
    var themeCover: String? = "" //主题封面
    var themeIntro: String? = "" //主题简介
    var bookCount: Int = 0       //经书数量
    var onShelfCount: Int = 0    //加入书架的人数
}
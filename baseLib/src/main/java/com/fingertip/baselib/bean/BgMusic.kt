package com.fingertip.baselib.bean

/**
 * 念佛页背景音乐配置（buddha_bg_music 表，随 globalParam 一次下发）
 */
class BgMusic {
    var id: Int = 0
    //曲名，如：梵音·清净海潮
    var musicTitle: String? = null
    //副文案，播放条第二行
    var musicDesc: String? = null
    //音频文件地址（完整 URL）
    var musicUrl: String? = null
}

package com.fingertip.baselib.bean

data class MediaInfo(
    val mediaType:Int, //文件类型 1 图片 2视频
    val mediaUrl:String, //本地路径
    val thumbUrl:String, //视频缩略图路径
    val width:Int, //图片宽度
    val height:Int, //图片高度
    val duration:Int, //视频时长:秒
    val size:Long //文件大小
)

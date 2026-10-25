package com.fingertip.baselib.net

import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.GsonUtils
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.top.TopUtils
import com.fingertip.baselib.util.DeviceIdUtils
import com.fingertip.baselib.util.HashUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

/**
 * 参数构造工具类
 */
object RequestBodyFactory {

    fun <K, V> buildJsonRequestBody(map: Map<K, V>): RequestBody {
        // 设置请求类型
        val mediaType = "application/json;charset=UTF-8".toMediaTypeOrNull()
        return GsonUtils.toJson(map).toRequestBody(mediaType)
    }

    private fun makeMd5Sign(vararg params: String): String {
        val builder = StringBuilder()
        params.forEach {
            builder.append(it)
        }
        return HashUtil.getMD5(builder.toString()).lowercase()
    }

    /**
     * 登录参数
     */
    fun loginBody(phoneNumber: String, psw: String): RequestBody {
        val param = HashMap<String, Any>()

        param["platform"] = NetProperty.PLATFORM
        param["channelId"] = NetProperty.CHANNEL
        param["clientVersion"] = DeviceIdUtils.getVersionName()
        param["deviceName"] = TopUtils.sjInfo
        param["deviceUUID"] = DeviceIdUtils.getDeviceChannleID(TopApplication.instance)
        param["phoneNumber"] = phoneNumber
        param["password"] = psw
        param["sign"] = makeMd5Sign(NetProperty.PLATFORM.toString(),NetProperty.CHANNEL.toString(),
            DeviceIdUtils.getVersionName(),DeviceIdUtils.getDeviceChannleID(TopApplication.instance), phoneNumber, psw, NetProperty.API_KEY)
        return buildJsonRequestBody(param)
    }


    /**
     * 发布动态入参
     */
    fun postMomentBody(textContent: String,videoUrl: String,videoCover: String,imageUrl: String,
                       videoDuration: Int,videoWidth: Int,videoHeight: Int,isAnonymous: Boolean,
                       location: String,remindAccountIds: List<Int>): RequestBody {
        val param = HashMap<String, Any>()

        param["textContent"] = textContent
        param["videoUrl"] = videoUrl
        param["videoDuration"] = videoDuration
        param["videoWidth"] = videoWidth
        param["videoHeight"] = videoHeight
        param["isAnonymous"] = isAnonymous
        param["location"] = location
        param["remindAccountIds"] = remindAccountIds
        param["videoCover"] = videoCover
        param["imageUrl"] = imageUrl
        return buildJsonRequestBody(param)
    }

    /**
     * 发布动态评论入参
     */
    fun postMomentBody(momentId: Int,parentCommentId: Int,targetCommentId: Int,textContent: String): RequestBody {
        val param = HashMap<String, Any>()

        param["momentId"] = momentId
        param["parentCommentId"] = parentCommentId
        param["targetCommentId"] = targetCommentId
        param["textContent"] = textContent
        return buildJsonRequestBody(param)
    }


    /**
     * 设置动态已读入参
     */
    fun viewMomentBody(momentIds: List<Int>): RequestBody {
        val param = HashMap<String, Any>()
        param["momentIds"] = momentIds
        return buildJsonRequestBody(param)
    }



    /**
     * 注册
     */
    fun bindAccountBody(accountName:String,password:String,nickName:String,gender:String,age:Int,avatar:String): RequestBody {
        val param = HashMap<String, Any>()
        param["accountName"] = accountName
        param["password"] = password
        param["nickName"] = nickName
        param["gender"] = gender
        param["age"] = age
        param["avatar"] = avatar
        param["deviceUUID"] = DeviceIdUtils.getDeviceChannleID(TopApplication.instance)
        param["countryCode"] = Locale.getDefault().country
        param["channelId"] = NetProperty.CHANNEL
        param["languageCode"] = Locale.getDefault().language
        param["mobileInfo"] = TopUtils.sjInfo

        return buildJsonRequestBody(param)
    }

    /**
     * 上传相册图片
     */
    fun uploadPictureBody(imgList:MutableList<String>): RequestBody {
        val map = HashMap<String, Array<String>>()
        val arr = imgList.toTypedArray()
        map["picture"] = arr

        return buildJsonRequestBody(map)
    }

    /**
     * 通话评论
     */
    fun buildCommentTagBody(tags: List<Int>): RequestBody {
        val param = HashMap<String, Any>()
        param["tag"] = tags.toTypedArray()
        return buildJsonRequestBody(param)
    }


    /**
     * 接听电话
     */
    fun buildAnswerBody(anchorId: Int, roomId: Int): RequestBody {
        val param = HashMap<String, Any>()
        param["anchorId"] = anchorId
        param["roomId"] = roomId
        return buildJsonRequestBody(param)
    }

    /**
     * 举报
     */
    fun buildReportBody(content: String, images: Array<String>): RequestBody {
        val param = HashMap<String, Any>()
        param["content"] = content
        param["images"] = images
        return buildJsonRequestBody(param)
    }

    /**
     * 读经进度
     */
    fun buildReadProgressBody(bookId: Int, seq: Int, charOffset: Int): RequestBody {
        val param = HashMap<String, Any>()
        param["bookId"] = bookId
        param["seq"] = seq
        param["charOffset"] = charOffset
        return buildJsonRequestBody(param)
    }

    /**
     * 抄经进度
     */
    fun buildCopyProgressBody(bookId: Int, seq: Int, charOffset: Int, chars: Int,
                              durationMs: Int, skippedChars: Int): RequestBody {
        val param = HashMap<String, Any>()
        param["bookId"] = bookId
        param["seq"] = seq
        param["charOffset"] = charOffset
        param["chars"] = chars
        param["durationMs"] = durationMs
        param["skippedChars"] = skippedChars
        return buildJsonRequestBody(param)
    }

    /**
     * 抄经设置
     */
    fun buildCopySettingBody(defaultColor: Int, copyColor: Int,
                             backgroundColor: Int, fontSizeLevel: Int): RequestBody {
        val param = HashMap<String, Any>()
        param["defaultColor"] = defaultColor
        param["copyColor"] = copyColor
        param["backgroundColor"] = backgroundColor
        param["fontSizeLevel"] = fontSizeLevel
        return buildJsonRequestBody(param)
    }

    /**
     * 读经设置
     */
    fun buildReadSettingBody(textColor: Int, backgroundColor: Int, fontSizeLevel: Int,
                             lineSpacingLevel: Int, keepScreenOn: Int, showTrad: Int): RequestBody {
        val param = HashMap<String, Any>()
        param["textColor"] = textColor
        param["backgroundColor"] = backgroundColor
        param["fontSizeLevel"] = fontSizeLevel
        param["lineSpacingLevel"] = lineSpacingLevel
        param["keepScreenOn"] = keepScreenOn
        param["showTrad"] = showTrad
        return buildJsonRequestBody(param)
    }

    /**
     * 许愿入参
     */
    fun wishBody(wishContent: String): RequestBody {
        val param = HashMap<String, Any>()
        param["wishContent"] = wishContent
        return buildJsonRequestBody(param)
    }

    /**
     * 送祝福入参
     */
    fun blessBody(wishingId: Int, blessContent: String): RequestBody {
        val param = HashMap<String, Any>()
        param["wishingId"] = wishingId
        param["blessContent"] = blessContent
        return buildJsonRequestBody(param)
    }

    /**
     * 愿望已读入参。服务端 @RequestBody List<Integer>，要裸 JSON 数组，如 [1,2,3]
     */
    fun wishReadBody(wishingIds: List<Int>): RequestBody {
        val mediaType = "application/json;charset=UTF-8".toMediaTypeOrNull()
        return GsonUtils.toJson(wishingIds).toRequestBody(mediaType)
    }
}
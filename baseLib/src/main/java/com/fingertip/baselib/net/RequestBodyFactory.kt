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

    private val secretKey = "55c9bc4c67638934fafa150c48bd4008"


    /**
     * 登陆参数
     */
    fun loginBody(account: String, psw: String): RequestBody {
        val param = HashMap<String, Any>()

        param["channelId"] = NetProperty.CHANNEL
        param["clientVersion"] = NetProperty.VERSION
        param["deviceName"] = DeviceUtils.getModel()
        param["mobileInfo"] = TopUtils.sjInfo
        param["deviceUUID"] = DeviceIdUtils.getDeviceChannleID(TopApplication.instance)
        param["accountName"] = account
        param["password"] = psw
        param["sign"] = makeMd5Sign(NetProperty.CHANNEL.toString(), secretKey, account, psw)

        return buildJsonRequestBody(param)
    }

    private fun makeMd5Sign(vararg params: String): String {
        val builder = StringBuilder()
        params.forEach {
            builder.append(it)
        }
        return HashUtil.getMD5(builder.toString()).toUpperCase(Locale.getDefault())
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
}
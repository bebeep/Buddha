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
     * 设备公共参数。海外版所有登录方式都要带，抽出来避免五处各写一遍。
     */
    private fun deviceParams(): HashMap<String, Any> {
        val param = HashMap<String, Any>()
        param["platform"] = NetProperty.PLATFORM
        param["channelId"] = NetProperty.CHANNEL
        param["clientVersion"] = DeviceIdUtils.getVersionName()
        param["deviceName"] = TopUtils.sjInfo
        param["deviceUUID"] = DeviceIdUtils.getDeviceChannleID(TopApplication.instance)
        // 时区随每次登录上报：用户换国家后早课跨天与推送时间要跟着变
        param["timeZone"] = TimeZone.getDefault().id
        return param
    }

    /**
     * 登录方式，与服务端 AccountServiceImpl.LOGIN_TYPE_* 对齐
     */
    const val LOGIN_TYPE_EMAIL = 1
    const val LOGIN_TYPE_GOOGLE = 2
    const val LOGIN_TYPE_APPLE = 3

    /**
     * 邮箱登录
     * @param email 邮箱
     * @param pswMd5 已做 MD5 的密码
     */
    fun emailLoginBody(email: String, pswMd5: String): RequestBody {
        val param = deviceParams()
        param["loginType"] = LOGIN_TYPE_EMAIL
        param["email"] = email.trim().lowercase()
        param["password"] = pswMd5
        return buildJsonRequestBody(param)
    }

    /**
     * 邮箱注册
     */
    fun registerBody(email: String, pswMd5: String): RequestBody {
        val param = deviceParams()
        param["loginType"] = LOGIN_TYPE_EMAIL
        param["email"] = email.trim().lowercase()
        param["password"] = pswMd5
        return buildJsonRequestBody(param)
    }

    /**
     * 第三方登录
     * @param loginType LOGIN_TYPE_GOOGLE / LOGIN_TYPE_APPLE
     * @param idToken Google idToken 或 Apple identityToken
     */
    fun thirdLoginBody(loginType: Int, idToken: String): RequestBody {
        val param = deviceParams()
        param["loginType"] = loginType
        param["thirdToken"] = idToken
        return buildJsonRequestBody(param)
    }

    /**
     * 绑定第三方账号 / 补绑邮箱
     */
    fun bindThirdBody(loginType: Int, email: String = "", idToken: String = ""): RequestBody {
        val param = deviceParams()
        param["loginType"] = loginType
        if (email.isNotEmpty()) param["email"] = email.trim().lowercase()
        if (idToken.isNotEmpty()) param["thirdToken"] = idToken
        return buildJsonRequestBody(param)
    }

    /**
     * 游客登录（保留：卸载重装后同一设备仍能找回未注册账号的数据）
     */
    fun guestLoginBody(): RequestBody {
        val param = deviceParams()
        param["sign"] = makeMd5Sign(
            NetProperty.PLATFORM.toString(), NetProperty.CHANNEL.toString(),
            DeviceIdUtils.getVersionName(), DeviceIdUtils.getDeviceChannleID(TopApplication.instance),
            NetProperty.API_KEY
        )
        return buildJsonRequestBody(param)
    }

    /**
     * 设备档案上报（离线推送 + 定时提醒的时区数据源）
     */
    fun deviceProfileBody(
        fcmToken: String,
        timeZone: String,
        lang: String
    ): RequestBody {
        val param = HashMap<String, Any>()
        param["fcmToken"] = fcmToken
        param["platform"] = NetProperty.PLATFORM
        param["timeZone"] = timeZone
        param["lang"] = lang
        return buildJsonRequestBody(param)
    }


    /**
     * 发布动态入参
     */
    fun postMomentBody(textContent: String,videoUrl: String,videoCover: String,imageUrl: String,
                       videoDuration: Int,videoWidth: Int,videoHeight: Int,isAnonymous: Boolean,
                       location: String,remindAccountIds: List<Int>,checkIn: Boolean = false): RequestBody {
        val param = HashMap<String, Any>()

        param["textContent"] = textContent
        param["videoUrl"] = videoUrl
        param["videoDuration"] = videoDuration
        param["videoWidth"] = videoWidth
        param["videoHeight"] = videoHeight
        param["isAnonymous"] = isAnonymous
        param["checkIn"] = checkIn
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
     * @param isAnonymous 是否匿名：0 否 1 是
     */
    fun wishBody(wishContent: String, isAnonymous: Int = 0): RequestBody {
        val param = HashMap<String, Any>()
        param["wishContent"] = wishContent
        param["isAnonymous"] = isAnonymous
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

    /**
     * 供佛入参（选择供奉天数）
     */
    fun worshipBody(buddhaId: Int, days: Int): RequestBody {
        val param = HashMap<String, Any>()
        param["buddhaId"] = buddhaId
        param["days"] = days
        return buildJsonRequestBody(param)
    }

    /**
     * 装扮佩戴入参。wear=true 佩戴，false 摘下
     * 对应服务端 WearReq{dressId, wear}
     */
    fun dressWearBody(dressId: Int, wear: Boolean): RequestBody {
        val param = HashMap<String, Any>()
        param["dressId"] = dressId
        param["wear"] = wear
        return buildJsonRequestBody(param)
    }

    /**
     * IAP 验单入参，对应服务端 IapVerifyReq{orderNo, platform, payOrderId}
     * @param orderNo 服务端 createOrder 返回的内部订单号
     * @param platform 1 Android 2 iOS
     * @param payOrderId 平台交易号（Google purchaseToken / Apple originalTransactionId）
     */
    fun iapVerifyBody(orderNo: String, platform: Int, payOrderId: String): RequestBody {
        val param = HashMap<String, Any>()
        param["orderNo"] = orderNo
        param["platform"] = platform
        param["payOrderId"] = payOrderId
        return buildJsonRequestBody(param)
    }

    /**
     * 恢复购买入参
     * @param payOrderIds 平台侧取回的交易号列表
     */
    fun restoreBody(payOrderIds: List<String>): RequestBody {
        val param = HashMap<String, Any>()
        param["payOrderIds"] = payOrderIds
        return buildJsonRequestBody(param)
    }

    /**
     * 早课提交入参
     * @param stepData 五步明细 json，形如 {"worship":3,"chantSeconds":62,"nianfo":21,"vow":"..."}
     * @param totalSeconds 从开始早课到提交的总秒数，服务端据此做时长下限校验
     * @param timeZone 用户时区 id，如 Asia/Singapore，服务端按它判定早课/晚课与"今天"
     */
    fun practiceSubmitBody(stepData: String, totalSeconds: Int, timeZone: String): RequestBody {
        val param = HashMap<String, Any>()
        param["stepData"] = stepData
        param["totalSeconds"] = totalSeconds
        param["timeZone"] = timeZone
        return buildJsonRequestBody(param)
    }

    /**
     * 念佛计数上报入参
     * @param countDate 记账日期（用户本地时区的 yyyy-MM-dd），服务端按它算"单日上限"
     */
    fun meditationReportBody(
        buddhaName: String,
        addCount: Int,
        addSeconds: Int,
        countDate: String
    ): RequestBody {
        val param = HashMap<String, Any>()
        param["buddhaName"] = buddhaName
        param["addCount"] = addCount
        param["addSeconds"] = addSeconds
        param["countDate"] = countDate
        return buildJsonRequestBody(param)
    }

    /**
     * 举报入参
     * @param targetAccountId 被举报内容的作者账号 id
     * @param reportType 举报场景 1动态 2动态评论
     * @param reportTag 举报原因标签（spam/porn/abuse/fraud/other）
     * @param reportContent 附加说明，可空（后台存 json 便于扩展）
     */
    fun reportBody(
        targetAccountId: Int,
        reportType: Int,
        reportTag: String,
        reportContent: String
    ): RequestBody {
        val param = HashMap<String, Any>()
        param["targetAccountId"] = targetAccountId
        param["reportType"] = reportType
        param["reportTag"] = reportTag
        param["reportContent"] = reportContent
        return buildJsonRequestBody(param)
    }

    /**
     * 收藏 / 取消收藏
     *
     * @param targetType 1佛经 2感悟 3寺庙
     * @param targetId 目标 id
     */
    fun collectBody(targetType: Int, targetId: Int): RequestBody {
        val param = HashMap<String, Any>()
        param["targetType"] = targetType
        param["targetId"] = targetId
        return buildJsonRequestBody(param)
    }

    /**
     * 保存佛历提醒规则
     *
     * @param id 0 新建，>0 更新
     * @param remindType 1节日 2斋日 3自定义
     * @param festivalKey 自定义提醒的文案，节日/斋日传空串
     * @param remindOffset 提前天数，0当天、-1前一天（只对节日提醒有效）
     * @param hour 0-23，用户本地时区的整点
     * @param enabled 0停用 1启用
     */
    fun remindBody(id: Int, remindType: Int, festivalKey: String, remindOffset: Int,
                   hour: Int, enabled: Int): RequestBody {
        val param = HashMap<String, Any>()
        param["id"] = id
        param["remindType"] = remindType
        param["festivalKey"] = festivalKey
        param["remindOffset"] = remindOffset
        // 服务端是 java.util.Date(HH:mm:ss)，带上秒避免两边对「07:00」的理解不一致
        param["remindTime"] = String.format(Locale.US, "%02d:00:00", hour)
        param["isEnabled"] = enabled
        return buildJsonRequestBody(param)
    }
}
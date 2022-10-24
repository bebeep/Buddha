package com.fingertip.baselib.bean


import kotlin.math.ceil

class PersonData : TopData {

    var accountId: Int = 0
    var anchorType: Int = 0
    var gender: String? = null
    var email: String? = null
    var nickName: String? = null
    var age: Int = 0
    var district: String? = null
    var avatar: String? = null
    var avatarInt: Int? = null
    var lineStatus: Int = 0         // 1:空闲（绿） 2: 勿扰（灰) 3：繁忙(红）
    var corver: String? = null
    var commentUp: Int = 0
    var commentDown: Int = 0
    var coins: Int = 0
    var mood: String? = null
    var displayAccountId: String? = null
    var corverWidth: Int = 0
    var corverHeight: Int = 0
    var callFee: Int = 0
    var loginStatus: Int = 0
    var ppa: String? = null
    var lastDrawDay: Int = 0
    var isHasMissedCall: Boolean = false
    var blacklistStatus: Int = 0
    var followers: Int = 0
    var followings: Int = 0
    var districtStatus: Int = 0
    var drawType: Int = 0
    var isFollowed: Boolean = false
    var isHot: Boolean = false
    var skilledLanguageList: SkilledLanguageListData? = null
    var anchorLevel: Int = 0
    var gifAvatar: String? = null
    var videoAvatar: String? = null
    var isVoiceChatRoomHost: Boolean = false
    var isSVip: Boolean = false
    var webPayCondition: Boolean = false;
    var customDisplayAccountId: String? = null
    var customAccountId: Int = 0
    var rechargeAmount: Float = 0f
    var isGGDiscountBought: Boolean = false
    var isVideoChatRoomHost: Boolean = false
    var IsDiscountBought: Boolean = false
    var isIndiaCurrencyOn: Boolean = false // / 是否可以显示印度货币
    var isApplyAnchor = false
    var live800ChatUrl: String? = null // live800 客服链接
    var privileges: List<Int>? =
        null  // 特权数组 1：头像框 2：初级宝箱 3：免费发消息 4：聊天室特效 5：高级宝箱 6：隐身功能 7：至尊头像框 8：免hunting
    var isNewStar = false
    var anchorFlag =
        0 // 主播标签 【HOT：anchorFlag&2>0】【BunnyGirl: anchorFlag&4>0】【NewStar: anchorFlag&8>0】【NewStar: anchorFlag&8>0】【AnniversaryStar: anchorFlag&16>0】


    var userType = 0 // 用户类型 0：主播  1：用户

    var abTestFlag=0;


    //登录账户/密码
    var loginPassword: String? = null
    var loginAccount: String? = null

    // im使用，是否是activity消息
    var isOfficial = false


    var multipleLive: MultipleLive? = null


    class MultipleLive : TopData() {
        var type = 0
        var roomId = 0
        var hostAccountId = 0
    }

    class SkilledLanguageListData : TopData() {
        var languageCode: List<Int>? = null
    }



    constructor(name: String, accoutId: Int, avatar: String?) {
        this.nickName = name
        this.accountId = accoutId
        this.avatar = avatar
    }


    //拉黑专用
    var objAccountId: Int = 0
    var objName: String? = ""
    var objAvatar: String? = ""
    fun initBlackBean() {
        this.nickName = objName
        this.accountId = objAccountId
        this.avatar = objAvatar
    }

    //关注记录专用
    var name: String = ""
    fun initFollowBean() {
        this.nickName = name
    }

    //通话记录专用
    var objAvatarUrl: String? = ""
    var objLineStatus: Int = 0
    var objLoginStatus: Int = 0
    var callTime: Long = 0L
    var callResult: Int = 0
    var callDuration: Long = 0L
    var objDisplayAccountId: String? = ""

    /**
    UxZCallsCallTypeOutcoming = 1,           // 用户呼叫
    UxZCallsCallTypeIncoming = 2,                // 主播呼叫
    UxZCallsCallTypeVoiceChatRoomPrivate = 3,    // 语聊室带走
    UxZCallsCallTypeChristmasCall = 4,           // 圣诞节特殊呼叫
    UxZCallsCallTypeLivePrivate = 5,             // Live带走
     */
    var callType: Int = 0


    var objAge: Int = 0
    var textColor: String = ""
    fun initCallListBean() {
        this.nickName = objName
        this.accountId = objAccountId
        this.avatar = objAvatarUrl
        this.lineStatus = objLineStatus
        this.loginStatus = objLoginStatus
        this.age = objAge
        this.displayAccountId = objDisplayAccountId
        var str = ""
        when (callResult) {
            1 -> {
                //正常接通
                var takeList = mutableListOf<Int>(1, 3, 5)
                if (callType in takeList) {
                    str = "Outgoing " + ceil(callDuration / 60.0)
                        .toInt() + "mins"
                } else {
                    str = "Incoming " + ceil(callDuration / 60.0)
                        .toInt() + "mins"
                }
                this.textColor = "#BBC3CE"
            }
            else -> {
                str = "Missed"
                this.textColor = "#F82E4B"
            }
        }
        this.mood = str
    }

    constructor() {
    }

    /**
     * 是否有语聊房入场特效
     */
    fun hasMultiRoomPrivilege() = privileges != null && privileges?.indexOf(4) != -1


    /**
     * 是否可以免费发消息
     */
    fun canSendMessageFree() = privileges != null && (privileges?.contains(3)?:false)

    override fun toString(): String {
        return "PP_PersonData(accountId=$accountId, nickName=$nickName, displayAccountId=$displayAccountId)"
    }
}
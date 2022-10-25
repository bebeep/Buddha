package com.fingertip.uilib.app

import android.app.NotificationManager
import android.app.Service
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LanguageUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baseLib.BuildConfig
import com.fingertip.baselib.language.LanguageUtil
import com.google.gson.Gson
import java.lang.Exception
import java.util.*


/**
 * 主application
 */
class UIApplication: AppApplication() {

    override fun onCreate() {
        super.onCreate()
        val local = LanguageUtils.getSystemLanguage()//当前系统语言
        val customLan = SPUtils.getInstance().getString("customLanguage")
        LogUtils.e("language===============${local.language}  |  $customLan")
        if (customLan.isNullOrEmpty()) {
//            LanguageUtils.applyLanguage(local,false)
            if (LanguageUtil.languages.contains(local.language))SPUtils.getInstance().put("customLanguage",local.language)
            else SPUtils.getInstance().put("customLanguage",LanguageUtil.languages[0])
        }else{
//            LanguageUtils.applyLanguage(Locale(customLan),false)
        }

        LogUtils.e("language===============${LanguageUtils.getAppContextLanguage().language}")


        if (BuildConfig.DEBUG){
            Debuger.enable()
            NSFWHelper.openDebugLog()
        }
        NSFWHelper.initHelper(context = TopApplication.instance,isOpenGPU = false)
    }

    private fun initFacebook() {
        FacebookSdk.setClientToken(getString(R.string.facebook_app_id))
        FacebookSdk.sdkInitialize(this)
        AppEventsLogger.activateApp(this)
//        FacebookSdk.setIsDebugEnabled(false)
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
    }


    private fun initOngSignal() {
        //OneSigal 推送的初始化
        OneSignal.startInit(this)
            .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
            .unsubscribeWhenNotificationsAreDisabled(true)
            .setNotificationReceivedHandler(ReceiveMsgHandler())
            .init()
        OneSignal.idsAvailable(OneSigalUerIDGetHandler())
        OneSignal.sendTag("roleType", "audience")
    }

    // onesigal 接收到消息的处理类
    private inner class ReceiveMsgHandler : OneSignal.NotificationReceivedHandler {
        override fun notificationReceived(msg: OSNotification) {
            // 构建新得弹窗
            val title = msg.payload.title
            val jsonBody = msg.toJSONObject().toString()
            val content = msg.payload.body

            log("OneSignal", "推送过来的消息title: $title     content: $content       body: $content-----------------$jsonBody")


            //Toast.makeText(InitialApplication.getInstance(),"OneSigle 推送过来的消息---title:" + title + "    " + "   body: " + content+";;Isruaning:"+  MyUtils.isRunning(),Toast.LENGTH_SHORT).show();
            //隐藏onesignal默认发送弹窗
            val manager = getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(msg.androidNotificationId)
            if (title == "Amazing Live-stream") {
                val pushData: PushData? = Gson().fromJson(jsonBody, PushData::class.java)
                log("OneSignal", "推送过来的消息Json: " + (pushData?.toString() ?: "解析为空"))
                pushData?.payload?.run {
                    additionalData?.let {
                        if (it.agoraRoomId==null){
                            return
                        }
                        NotifyDelegate.showNotifyWithAvatar(
                            displayId = it.agoraRoomId ?: "0",
                            accountId = it.host,
                            nickName = this.title ?: "",
                            textContent = this.body ?: "",
                            avatar = it.corver,
                            notifyType = PP_Constants.PushType.Room
                        )
                    }

                }

            }

            if (title == "Message" && !AppUtils.isAppRunning(packageName)) { // IM消息
                val str =
                    content.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val othserDisplayId = str[1]
                val waht = str[0]
                val nikename = str[2]
                var accId: Int = 0
                if (str.size > 3)
                    accId = str[3].toInt()

                var avatar = ""
                if (str.size > 4) {
                    avatar = str[4]
                }
                // com.video.baselibrary.push.utils.NotificationUtil.showNotificationMy(instance, nikename, waht, othserDisplayId,accId)
                NotifyDelegate.showNotifyWithAvatar(
                    displayId = othserDisplayId,
                    accountId = accId,
                    nickName = nikename, textContent = waht,
                    avatar = avatar,
                    notifyType =  PP_Constants.PushType.Message
                )
            } else {
                val additionalData = msg.payload.additionalData ?: return

                if (additionalData.getString("type") == "AnchorOnline") {
                    val displayId = additionalData.getString("DisplayAccountId")
                    val accountId = additionalData.getInt("AnchorId")
                    val avatar = additionalData.getString("Avatar")
                    val anchorName = additionalData.getString("AnchorName")

                    NotifyDelegate.showNotifyWithAvatar(
                        displayId,
                        accountId,
                        "$anchorName is ONLINE",
                        content,
                        avatar,
                        PP_Constants.PushType.AnchorOnLine
                    )
                }

                if (additionalData.getString("type") == "FollowingMomentUpdated"){//moment
                    try {
                        val momentId = additionalData.getJSONObject("moment").getInt("momentId")
                        val avatar = additionalData.getJSONObject("moment").getString("avatar")
                        val nickname = additionalData.getJSONObject("moment").getString("nickName")
                        NotifyDelegate.showNotify(
                            "-88",
                            momentId,
                            "Zaki Chat",
                            content,
                            avatar,
                            null,
                            PP_Constants.PushType.Moment
                        )
                    }catch (e: Exception){}
                }

            }
        }
    }

    //获取设备的onesigal id
    private inner class OneSigalUerIDGetHandler : OneSignal.IdsAvailableHandler {
        override fun idsAvailable(userId: String?, registrationId: String?) {
            // ONE_SIGNAL_USER_ID=userId
            YHManager.PUSH_ID = userId
            log("OneSignal", "获取用户的id:  userID：$userId   registrationID: $registrationId")
            YHManager.setOneSignalId()
        }
    }
}
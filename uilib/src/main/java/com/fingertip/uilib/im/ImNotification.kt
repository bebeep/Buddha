package com.fingertip.uilib.im

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopApplication
import com.fingertip.uilib.R
import com.tencent.imsdk.v2.V2TIMMessage

/**
 * 前台收到新消息时的本地通知。
 *
 * 离线通知由腾讯云 IM 后台直接走厂商通道（FCM），不经这里；
 * 本类只覆盖「App 在前台但用户没在对应会话」的场景——这种状态厂商通道不会发，
 * 不补一条用户就完全感知不到新消息。
 */
object ImNotification {

    private const val TAG = "ImNotification"
    private const val CHANNEL_ID = "im_message"
    private const val CHANNEL_NAME = "消息通知"
    private var lastNotifyAt = 0L

    fun notifyNewMessage(msg: V2TIMMessage) {
        val context: Context = try {
            TopApplication.instance
        } catch (e: Exception) {
            return
        }
        //简单节流：短时间内多条消息只弹一条，避免刷屏
        val now = System.currentTimeMillis()
        if (now - lastNotifyAt < 1500) return
        lastNotifyAt = now

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
            //点通知回到主界面，消息 tab 有角标引导
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pending = PendingIntent.getActivity(
                context, 1001,
                intent ?: Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val title = msg.nickName?.takeIf { it.isNotBlank() } ?: msg.sender ?: ""
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                android.app.Notification.Builder(context)
            }
            builder.setContentTitle(title)
                .setContentText(ImMsgUtil.summaryOf(msg))
                .setSmallIcon(com.fingertip.baselib.R.drawable.icon_tab_post_u)
                .setAutoCancel(true)
                .setContentIntent(pending)
            nm.notify(1001, builder.build())
        } catch (e: Exception) {
            log(TAG, "本地通知失败 ${e.message}")
        }
    }
}

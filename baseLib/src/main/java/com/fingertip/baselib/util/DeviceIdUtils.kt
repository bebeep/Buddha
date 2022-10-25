package com.fingertip.baselib.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.log
import com.fingertip.baselib.net.NetProperty
import okhttp3.internal.and
import java.lang.Exception
import java.lang.StringBuilder
import java.security.MessageDigest
import java.util.*

object DeviceIdUtils {
    /*
     * deviceID的组成为：渠道标志+识别符来源标志+hash后的终端识别符
     *
     * 渠道标志为：
     * 1，andriod（a）
     *
     * 识别符来源标志：
     * 1， wifi mac地址（wifi）；
     * 2， IMEI（imei）；
     * 3， 序列号（sn）；
     * 4， id：随机码。若前面的都取不到时，则随机生成一个随机码，需要缓存。
     *
     * @param context
     * @return
     */
    fun getDeviceId(context: Context): String {
        val deviceId = StringBuilder()
        // 渠道标志
        try {
            val ANDROID_ID =
                Settings.System.getString(context.contentResolver, Settings.System.ANDROID_ID)
            if (!ANDROID_ID.isEmpty() || ANDROID_ID == "android_id") {
                deviceId.append(ANDROID_ID)
                log("getDeviceId_androidid : ", deviceId.toString())
                return deviceId.toString() + testAddId
            }
            //如果上面都没有， 则生成一个id：随机码
            val uuid = getUUID(context)
            if (!TextUtils.isEmpty(uuid)) {
                deviceId.append("id")
                deviceId.append(uuid)
                log("getDeviceId_uuid : ", deviceId.toString())
                return deviceId.toString() + testAddId
            }
        } catch (e: Exception) {
            e.printStackTrace()
            deviceId.append("id").append(getUUID(context))
        }
        log("getDeviceId : ", deviceId.toString())
        return deviceId.toString() + testAddId
    }

    var testAddId = ""
    fun getDeviceChannleID(context: Context): String {
        val deviceId = getDeviceIdNew(context)
        LogUtils.e("getDeviceIdNew =================SERIAL:" + deviceId + "|" + getDeviceId(context))
        log("getDeviceChannleID : ", getDeviceId(context) + "_channel_" + NetProperty.CHANNEL)
        return getDeviceId(context) + "_channel_" + NetProperty.CHANNEL
    }

    /**
     * 得到全局唯一UUID
     */
    fun getUUID(context: Context?): String {
        var uuid: String = SPUtils.getInstance().getString("uuid")
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString()
            SPUtils.getInstance().put("uuid", uuid)
        }
        log("getUUID : ", uuid)
        return uuid
    }

    /**
     * 获取国家代码
     */
    val countryCode: String
        get() = Locale.getDefault().country

    /** */
    fun getDeviceIdNew(context: Context): String? {
        val sbDeviceId = StringBuilder()
        //        imei null
        val imei = getIMEI(context)
        //        imei null

//        手机型号 +手机
        val androidID = getAndroidId(context)

//        serial
//         唯一
        val serial = serial
        //        UUID  uuid----》
        val id = deviceUUID.replace("-", "")
        //追加imei
        if (imei != null && imei.length > 0) {
            sbDeviceId.append(imei)
            sbDeviceId.append("|")
        }
        //追加androidid
        if (androidID != null && androidID.length > 0) {
            sbDeviceId.append(androidID)
            sbDeviceId.append("|")
        }
        //追加serial
        if (serial != null && serial.length > 0) {
            sbDeviceId.append(serial)
            sbDeviceId.append("|")
        }
        //追加硬件uuid
        if (id != null && id.length > 0) {
            sbDeviceId.append(id)
        }
        //        一系列的字符串  ----11 硬件标识有关   手机
        //生成SHA1，统一DeviceId长度
        if (sbDeviceId.length > 0) {
//                    md  ----
            try {
                val hash = getHashByString(sbDeviceId.toString())
                val sha1 = bytesToHex(hash)
                if (sha1 != null && sha1.length > 0) {
                    //返回最终的DeviceId
                    return sha1
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        return null
    }

    /**
     * 转16进制字符串
     *
     * @param data 数据
     * @return 16进制字符串
     */
    private fun bytesToHex(data: ByteArray): String {
        val sb = StringBuilder()
        var stmp: String
        for (n in data.indices) {
            stmp = Integer.toHexString(data[n] and 0xFF)
            if (stmp.length == 1) sb.append("0")
            sb.append(stmp)
        }
        return sb.toString().toUpperCase(Locale.CHINA)
    }

    /**
     * 取SHA1
     *
     * @param data 数据
     * @return 对应的hash值
     */
    private fun getHashByString(data: String): ByteArray {
        return try {
            val messageDigest = MessageDigest.getInstance("SHA1")
            messageDigest.reset()
            messageDigest.update(data.toByteArray(charset("UTF-8")))
            messageDigest.digest()
        } catch (e: Exception) {
            "".toByteArray()
        }
    }

    // //获得硬件uuid（根据硬件相关属性，生成uuid）（无需权限）  数字  0   -10
    private val deviceUUID: String
        private get() {
            val dev = "100001" + Build.BOARD +
                    Build.BRAND +
                    Build.DEVICE +
                    Build.HARDWARE +
                    Build.ID +
                    Build.MODEL +
                    Build.PRODUCT +
                    Build.SERIAL
            return UUID(dev.hashCode().toLong(), Build.SERIAL.hashCode().toLong()).toString()
        }
    private val serial: String?
        private get() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return Build.getSerial()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return null
        }

    /**
     * 获得设备的AndroidId
     *
     * @param context 上下文
     * @return 设备的AndroidId
     */
    private fun getAndroidId(context: Context): String {
        try {
            return Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return ""
    }

    //需要获得READ_PHONE_STATE权限，>=6.0，默认返回null
    private fun getIMEI(context: Context): String {
        try {
            val tm: TelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return tm.getDeviceId()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return ""
    }
}
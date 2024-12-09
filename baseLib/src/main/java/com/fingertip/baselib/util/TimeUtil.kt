package com.fingertip.baselib.util

import com.fingertip.baselib.top.TopApplication
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Zzm丶Fiona on 2018/1/31.
 */
object TimeUtil {

    fun getDataMy(time: Long): String {
        val calendar: Calendar = GregorianCalendar()
        calendar[2017, 0, 1, 0, 0] = 0
        val calendar2: Calendar = GregorianCalendar()
        calendar2.timeInMillis = calendar.timeInMillis + time * 1000 + 8 * 60 * 60 * 1000
        val year = calendar2[Calendar.YEAR]
        var month = calendar2[Calendar.MONTH]
        val day = calendar2[Calendar.DAY_OF_MONTH]
        val hour = calendar2[Calendar.HOUR_OF_DAY] //24小时制
        val minute = calendar2[Calendar.MINUTE]
        val second = calendar2[Calendar.SECOND]
        var dayStr = day.toString()
        var monthStr: String = (++month).toString()
        var hourStr = hour.toString()
        var minuteStr = minute.toString()
        var sencodStr = second.toString()
        if (day < 10) dayStr = "0$day"
        if (month < 10) monthStr = "0$month"
        if (minute < 10) minuteStr = "0$minute"
        if (hour < 10) hourStr = "0$hour"
        if (second < 10) sencodStr = "0$second"
        return "$monthStr-$dayStr-$year $hourStr:$minuteStr:$sencodStr"
    }


    fun dateFormatTime(time: Long):String{
        getDate(time)?.let {
            return dateFormatTime(it)
        }
        return ""
    }

    /**
     * 日期格式化
     * @return
     */
    fun dateFormatTime(date: Date): String {
        //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //转换为日期
        val time = date.time
        return if (isThisYear(date)) { //今年
            val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            if (isToday(date)) { //今天
                val minute = minutesAgo(time)
                if (minute < 60) { //1小时之内
                    if (minute < 1) { //一分钟之内，显示刚刚
                        "刚刚"
                    } else {
                         "${minute}分钟之前"
                    }
                } else {
                    "今天${simpleDateFormat.format(date)}"
                }
            } else {
                if (isYestYesterday(date)) { //昨天，显示昨天
                    return "昨天${simpleDateFormat.format(date)}"
                }
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                sdf.format(date)

            }
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            sdf.format(date)
        }
    }

    private fun minutesAgo(time: Long): Int {
        return ((System.currentTimeMillis() - time) / 60000).toInt()
    }

    fun isToday(date: Date): Boolean {
        val now = Date()
        return date.year == now.year && date.month == now.month && date.date == now.date
    }

    private fun isYestYesterday(date: Date): Boolean {
        val now = Date()
        return date.year == now.year && date.month == now.month && date.date + 1 == now.date
    }

    private fun isThisWeek(date: Date): Boolean {
        val now = Date()
        if (date.year == now.year && date.month == now.month) {
            if (now.day - date.day < now.day && now.date - date.date > 0 && now.date - date.date < 7) {
                return true
            }
        }
        return false
    }

    private fun isThisYear(date: Date): Boolean {
        val now = Date()
        return date.year == now.year
    }

    fun getDate(time: Long): Date? {
        return getTime(getDataMy(time))
    }

    private fun getTime(talktime: String): Date? {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        var date_util: Date? = null //转换为util.date
        val a: Long? = null
        val b: Long? = null
        try {
            date_util = sdf.parse(talktime)
            return date_util
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return date_util
    }

    fun getHowLongTime(date: Date): String {
        var timeHowlongTime = ""
        timeHowlongTime = if (isoveroneday(date)) {
            return timeHowlongTime
        } else {
            TimeDiff(date)
        }
        return timeHowlongTime
    }

    fun isoveroneday(date: Date): Boolean {
        val date2 = Date()
        val between = date2.time - date.time
        return between > 24 * 3600000
    }

    /**
     *      * 判断2个时间相差多少天、多少小时、多少分<br></br>
     *      * <br></br>
     *      * @param pBeginTime 请假开始时间<br></br>
     *      * @param pEndTime 请假结束时间<br></br>
     *      * @return String 计算结果<br></br>
     *      * @Exception 发生异常<br></br>
     *      
     */
    fun TimeDiff(date: Date): String {
        val beginL = date.time
        val endL = Date().time
        val day = (endL - beginL) / 86400000
        val hour = (endL - beginL) % 86400000 / 3600000
        val min = (endL - beginL) % 86400000 % 3600000 / 60000
        val seconds = (endL - beginL) % 86400000 % 3600000 % 60000 / 1000
        var hourStr = ""
        var minStr = ""
        var secondStr = ""
        if (hour > 0) {
            hourStr = if (hour in 1..9) {
                "0$hour:"
            } else {
                "$hour:"
            }
        }
        minStr = if (min < 10) {
            "0$min:"
        } else {
            "$min:"
        }
        secondStr = if (seconds < 10) {
            "0$seconds"
        } else {
            seconds.toString() + ""
        }
        return hourStr + minStr + secondStr
    }

    fun getCurrTimestamp(patten: String?): String {
        val format = SimpleDateFormat(patten, Locale.ENGLISH)
        return format.format(Date(System.currentTimeMillis()))
    }

    /**
     * dd/MM/yyyy HH:mm:ss
     */
    fun formatTime(date: Date?, pattern: String?): String {
        val dateformat = SimpleDateFormat(pattern, Locale.ENGLISH)
        return dateformat.format(date)
    }

    /**
     * dd/MM/yyyy HH:mm:ss
     */
    fun formatTime(time: Long, pattern: String?): String {
        val dateformat = SimpleDateFormat(pattern, Locale.ENGLISH)
        return dateformat.format(Date(time))
    }

    fun formatTime(timeStr: String?, pattern: String?): Date? {
        val dateformat = SimpleDateFormat(pattern, Locale.ENGLISH)
        try {
            return dateformat.parse(timeStr)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取指定时间的00:00 或者23:59
     * @param date
     * @param flag
     * @return
     */
    fun dateToStringBeginOrEnd(date: Date?, flag: Boolean): Date? {
        var d: Date? = null
        val calendar1 = Calendar.getInstance()
        //获取某一天的0点0分0秒 或者 23点59分59秒
        if (flag) {
            calendar1.time = date
            calendar1[calendar1[Calendar.YEAR], calendar1[Calendar.MONTH], calendar1[Calendar.DAY_OF_MONTH], 0, 0] =
                0
            d = calendar1.time
        } else {
            val calendar2 = Calendar.getInstance()
            calendar2.time = date
            calendar1[calendar2[Calendar.YEAR], calendar2[Calendar.MONTH], calendar2[Calendar.DAY_OF_MONTH], 23, 59] =
                59
            d = calendar1.time
        }
        return d
    }

    /**
     * 获取指定时间是当前时间的几天前
     * @param date
     * @return
     */
    fun getTimeOffsetNow(date: Date): Int {
        val time = dateToStringBeginOrEnd(date, false)!!.time
        val l = System.currentTimeMillis()
        val delta = l - time
        val oneDayTime = (24 * 60 * 60 * 1000).toLong()
        return Math.ceil(delta * 1.0 / oneDayTime).toInt()
    }

    /**
     * 获取当前utc时间
     * @return
     */
    val currTimeUtcDate: Date?
        get() {
            val dateformat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            dateformat.timeZone = TimeZone.getTimeZone("GMT")
            val currTimeStr = dateformat.format(Date())
            dateformat.timeZone = Calendar.getInstance().timeZone
            try {
                return dateformat.parse(currTimeStr)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            return null
        }


    /**
     * 获取今天的日期
     */
    val todayTimeStr:String
        get(){
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            return dateFormat.format(Date())
        }

    /**
     * 获取制定时间的utc时间date
     * @param time
     * @return
     */
    fun getTimeUtcDate(time: Int): Date {
        val calendar: Calendar = GregorianCalendar()
        calendar[2017, 0, 1, 0, 0] = 0
        val calendar2: Calendar = GregorianCalendar()
        calendar2.timeInMillis = calendar.timeInMillis + time * 1000L+ 8 * 60 * 60 * 1000
        return calendar2.time
    }

    fun getTimeOffsetDayFrom2017(time: Long): Int {
        val calendar: Calendar = GregorianCalendar()
        calendar[2017, 0, 1, 0] = 0
        val calendar2: Calendar = GregorianCalendar()
        calendar2.timeInMillis = calendar.timeInMillis + time * 1000 + 8 * 60 * 60 * 1000
        val l = calendar2.timeInMillis - System.currentTimeMillis()
        var day = Math.floor(l * 1.0 / 1000 / 60 / 60 / 24).toInt()
        if (day < 0) {
            day = 0
        }
        return day
    }

    fun getTimeOffsetSecondsFrom2017(time: Long): Long {
        val calendar: Calendar = GregorianCalendar()
        calendar.timeZone = TimeZone.getTimeZone("GMT")
        calendar[2017, 0, 1, 0, 0] = 0
        val calendar2: Calendar = GregorianCalendar()
        calendar2.timeInMillis = calendar.timeInMillis + time * 1000
        val l = System.currentTimeMillis() - calendar2.time.time
        return (l / 1000f).toLong()
    }


    /**
     * 倒计时
     */
    fun parseDuration(duration: Int): String {
        if (duration <= 0) {
            return "00:00"
        }

        var result = ""
        val hour = duration / 60 / 60
        val min = duration / 60 % 60
        val sec = duration % 60

        if (hour > 0) {
            result += if (hour >= 10) {
                "$hour:"
            } else {
                "0$hour:"
            }
        }

        result += if (min >= 10) {
            "$min:"
        } else {
            "0$min:"
        }

        result += if (sec >= 10) {
            "$sec"
        } else {
            "0$sec"
        }
        return String.format(Locale.ENGLISH,result)
    }
}
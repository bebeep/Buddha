package iknow.android.utils;

import android.text.format.DateUtils;

public class DateUtil {

    public static String convertSecondsToTime(long seconds) {
        long minute = seconds / 60;
        long second = seconds % 60;
        if (minute < 60) {
            return String.format("00:%02d:%02d", minute, second);
        } else {
            long hour = minute / 60;
            if (hour > 99) return "99:59:59";
            minute = minute % 60;
            second = seconds - hour * 3600 - minute * 60;
            return String.format("%02d:%02d:%02d", hour, minute, second);
        }
    }
}

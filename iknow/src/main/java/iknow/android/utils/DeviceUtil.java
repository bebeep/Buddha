package iknow.android.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import iknow.android.utils.BaseUtils;

public class DeviceUtil {

    public static int getDeviceWidth() {
        DisplayMetrics dm = BaseUtils.getContext().getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    public static int getDeviceHeight() {
        DisplayMetrics dm = BaseUtils.getContext().getResources().getDisplayMetrics();
        return dm.heightPixels;
    }
}

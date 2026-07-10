package iknow.android.utils;

import android.app.Application;
import android.content.Context;

public class BaseUtils {
    private static Application sApp;

    public static void init(Application app) {
        sApp = app;
    }

    public static Application getApp() {
        return sApp;
    }

    public static Context getContext() {
        return sApp;
    }
}

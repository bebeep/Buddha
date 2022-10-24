package com.fingertip.baselib.util

import android.R
import android.app.Activity
import android.os.Build
import com.fingertip.baselib.util.FullTopUtil
import android.view.WindowManager
import com.fingertip.baselib.util.FullTopUtil.ViewType
import android.view.ViewGroup
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import androidx.annotation.IntDef
import java.lang.Exception
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * 状态栏相关工具类
 *
 */
object FullTopUtil {
    const val TYPE_MIUI = 0
    const val TYPE_FLYME = 1
    const val TYPE_M = 3 //6.0

    /**
     * 设置状态栏深色浅色切换
     */
    fun setStatusBarDarkTheme(activity: Activity, dark: Boolean): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setStatusBarFontIconDark(activity, TYPE_M, dark)
            } else { //其他情况
                return false
            }
            return true
        }
        return false
    }

    /**
     * 设置状态栏透明
     */
    fun setStatusTranslucent(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
            val window = activity.window
            val decorView = window.decorView
            //两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
            val option = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            decorView.systemUiVisibility = option
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            //导航栏颜色也可以正常设置
            //window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    /**
     * 设置 状态栏深色浅色切换
     */
    fun setStatusBarFontIconDark(activity: Activity, @ViewType type: Int, dark: Boolean): Boolean {
        return when (type) {
            TYPE_MIUI -> setMiuiUI(activity, dark)
            TYPE_FLYME -> setFlymeUI(activity, dark)
            TYPE_M -> setCommonUI(activity, dark)
            else -> setCommonUI(activity, dark)
        }
    }

    //设置6.0 状态栏深色浅色切换
    fun setCommonUI(activity: Activity, dark: Boolean): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = activity.window.decorView
            if (decorView != null) {
                var vis = decorView.systemUiVisibility
                vis = if (dark) {
                    vis or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    vis and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
                if (decorView.systemUiVisibility != vis) {
                    decorView.systemUiVisibility = vis
                }
                return true
            }
        }
        return false
    }

    /**
     * 代码实现android:fitsSystemWindows
     *
     * @param activity
     */
    fun FitsSystemWindows(activity: Activity, fitSystemWindows: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val winContent = activity.findViewById<View>(R.id.content) as ViewGroup
            if (winContent.childCount > 0) {
                val rootView = winContent.getChildAt(0) as ViewGroup
                if (rootView != null) {
                    rootView.fitsSystemWindows = fitSystemWindows
                }
            }
        }
    }

    //设置Flyme 状态栏深色浅色切换
    fun setFlymeUI(activity: Activity, dark: Boolean): Boolean {
        return try {
            val window = activity.window
            val lp = window.attributes
            val darkFlag =
                WindowManager.LayoutParams::class.java.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
            val meizuFlags = WindowManager.LayoutParams::class.java.getDeclaredField("meizuFlags")
            darkFlag.isAccessible = true
            meizuFlags.isAccessible = true
            val bit = darkFlag.getInt(null)
            var value = meizuFlags.getInt(lp)
            value = if (dark) {
                value or bit
            } else {
                value and bit.inv()
            }
            meizuFlags.setInt(lp, value)
            window.attributes = lp
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    //设置MIUI 状态栏深色浅色切换
    fun setMiuiUI(activity: Activity, dark: Boolean): Boolean {
        return try {
            val window = activity.window
            val clazz: Class<*> = activity.window.javaClass
            @SuppressLint("PrivateApi") val layoutParams =
                Class.forName("android.view.MiuiWindowManager\$LayoutParams")
            val field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
            val darkModeFlag = field.getInt(layoutParams)
            val extraFlagField = clazz.getDeclaredMethod(
                "setExtraFlags",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            extraFlagField.isAccessible = true
            if (dark) {    //状态栏亮色且黑色字体
                extraFlagField.invoke(window, darkModeFlag, darkModeFlag)
            } else {
                extraFlagField.invoke(window, 0, darkModeFlag)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun setWindowStatusBarColor(activity: Activity, colorResId: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window = activity.window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = activity.resources.getColor(colorResId)

                //底部导航栏
                //window.setNavigationBarColor(activity.getResources().getColor(colorResId));此项目不用
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @IntDef(TYPE_MIUI, TYPE_FLYME, TYPE_M)
    @Retention(RetentionPolicy.SOURCE)
    internal annotation class ViewType
}
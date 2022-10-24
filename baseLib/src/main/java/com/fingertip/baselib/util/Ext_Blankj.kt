package com.fingertip.baselib.util

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.IdRes
import com.blankj.utilcode.util.KeyboardUtils
import java.lang.ref.WeakReference

fun registerSoftInputChangedListener(
    window: Window,
    listener: KeyboardUtils.OnSoftInputChangedListener,
    @IdRes tag: Int
) {
    val flags = window.attributes.flags
    if (flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS != 0) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    val funGetDecorViewInvisibleHeight =  KeyboardUtils::class.java.getDeclaredMethod("getDecorViewInvisibleHeight",
        Window::class.java)
    funGetDecorViewInvisibleHeight.isAccessible = true

    val contentView = window.findViewById<FrameLayout>(android.R.id.content)
    val decorViewInvisibleHeightPre = intArrayOf(funGetDecorViewInvisibleHeight.invoke(null, window) as Int)
    val onGlobalLayoutListener = ExtOnGlobalLayoutListener(listener, window, decorViewInvisibleHeightPre)
    contentView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    contentView.setTag(tag, onGlobalLayoutListener)
}

@SuppressLint("ObsoleteSdkInt")
fun unregisterSoftInputChangedListener(window: Window, @IdRes tag: Int) {
    val contentView = window.findViewById<FrameLayout>(android.R.id.content)
    val _tag = contentView.getTag(tag)
    if (_tag is ExtOnGlobalLayoutListener) {
        _tag.listenerRef = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            contentView.viewTreeObserver.removeOnGlobalLayoutListener(_tag)
        }
    }
}

class ExtOnGlobalLayoutListener(
    listener: KeyboardUtils.OnSoftInputChangedListener,
    var window: Window,
    var decorViewInvisibleHeightPre: IntArray
): OnGlobalLayoutListener {

    var listenerRef: WeakReference<KeyboardUtils.OnSoftInputChangedListener>? = null

    init {
        listenerRef = WeakReference(listener)
    }

    private val funGetDecorViewInvisibleHeight =  KeyboardUtils::class.java.getDeclaredMethod("getDecorViewInvisibleHeight",
        Window::class.java).apply { isAccessible = true }

    override fun onGlobalLayout() {
        val height = funGetDecorViewInvisibleHeight.invoke(null, window) as Int
        if (decorViewInvisibleHeightPre[0] != height) {
            listenerRef?.get()?.onSoftInputChanged(height)
            decorViewInvisibleHeightPre[0] = height
        }
    }
}
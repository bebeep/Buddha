package com.lzlz.toplib.extention

import android.graphics.Color
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.annotation.Px
import com.blankj.utilcode.util.SizeUtils
import androidx.core.graphics.toColorInt


/**
 * Created by luyao
 * on 2019/7/9 9:45
 */

/**
 * Set view visible
 */
fun View.visible() {
    visibility = View.VISIBLE
}

/**
 * Set view invisible
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Set view gone
 */
fun View.gone() {
    visibility = View.GONE
}

/**
 * Reverse the view's visibility
 */
fun View.reverseVisibility(needGone: Boolean = true) {
    if (isVisible) {
        if (needGone) gone() else invisible()
    } else visible()
}

var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) = if (value) visible() else gone()

var View.isInvisible: Boolean
    get() = visibility == View.INVISIBLE
    set(value) = if (value) invisible() else visible()

var View.isGone: Boolean
    get() = visibility == View.GONE
    set(value) = if (value) gone() else visible()

/**
 * 获取view在屏幕中的位置
 */
fun View.getLocationOnScreen(): IntArray {
    val arr = IntArray(2)
    this.getLocationOnScreen(arr)
    return arr
}

/**
 * Set padding
 * @param size top, bottom, left, right padding are same
 */
fun View.setPadding(@Px size: Int) {
    setPadding(size, size, size, size)
}


inline fun View.onGlobalLayout(crossinline callback: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            callback()
        }
    })
}

inline fun View.afterMeasured(crossinline callback: View.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                callback()
            }
        }
    })
}

fun View.setMarginLeft(pxMargin: Int) {
    this.setMargin(left = pxMargin)
}

fun View.setMarginTop(pxMargin: Int) {
    this.setMargin(top = pxMargin)
}

fun View.setMarginRight(pxMargin: Int) {
    this.setMargin(right = pxMargin)
}

fun View.setMarginBottom(pxMargin: Int) {
    this.setMargin(bottom = pxMargin)
}

fun View.setMarginLeft(dpMargin: Float) {
    this.setMargin(left = SizeUtils.dp2px(dpMargin))
}

fun View.setMarginTop(dpMargin: Float) {
    this.setMargin(top = SizeUtils.dp2px( dpMargin))
}

fun View.setMarginRight(dpMargin: Float) {
    this.setMargin(right = SizeUtils.dp2px( dpMargin))
}

fun View.setMarginBottom(dpMargin: Float) {
    this.setMargin(bottom = SizeUtils.dp2px(dpMargin))
}

fun View.setMargin(margin: Float? = null) = setMargin(margin, margin, margin, margin)

fun View.setMargin(left: Float? = null, top: Float? = null, right: Float? = null, bottom: Float? = null) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val marginLayoutParam = layoutParams as ViewGroup.MarginLayoutParams
        marginLayoutParam.apply {
            left?.run { marginStart = left.toPx() }
            top?.run { topMargin = top.toPx() }
            right?.run { marginEnd = right.toPx() }
            bottom?.run { bottomMargin = bottom.toPx() }
        }
        layoutParams = marginLayoutParam
    }
}

fun View.setMargin(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val marginLayoutParam = layoutParams as ViewGroup.MarginLayoutParams
        marginLayoutParam.apply {
            left?.run { marginStart = left }
            top?.run { topMargin = top }
            right?.run { marginEnd = right }
            bottom?.run { bottomMargin = bottom }
        }
        layoutParams = marginLayoutParam
    }
}

fun View.setSize(w: Float? = null, h: Float? = null) {
    setSize(w?.toPx(), h?.toPx())
}

fun View.setSize(w: Int? = null, h: Int? = null) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val marginLayoutParam = layoutParams as ViewGroup.MarginLayoutParams
        marginLayoutParam.apply {
            w?.run { width = w }
            h?.run { height = h}
        }
        layoutParams = marginLayoutParam
    }
}

fun View.setWidth(width: Int) {
    setSize(w = width)
}

fun View.setWidth(width: Float) {
    setWidth(width.toPx())
}

fun View.setHeight(height: Int) {
    setSize(h = height)
}

fun View.setHeight(height: Float) {
    setHeight(height.toPx())
}

/**
 * 是否滚动底部
 */
fun RecyclerView.isScrollBottom(): Boolean = isRcScrollBottom(this)
fun isRcScrollBottom(recyclerView: RecyclerView): Boolean {
    val layoutManager = recyclerView.layoutManager
    if (layoutManager is LinearLayoutManager) {
        val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
        //屏幕中最后一个可见子项的position
        val lastVisibleItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()
        //当前屏幕所看到的子项个数
        val visibleItemCount = linearLayoutManager.childCount
        //当前RecyclerView的所有子项个数
        val totalItemCount = linearLayoutManager.itemCount
        //RecyclerView的滑动状态
        val state: Int = recyclerView.scrollState

        if (lastVisibleItemPosition == -1) {
            return true
        }

        if (visibleItemCount > 0 && lastVisibleItemPosition == totalItemCount - 1 && state == RecyclerView.SCROLL_STATE_IDLE) {
            return true
        }
    }
    return false
}

fun Int.toPx() = SizeUtils.dp2px(this.toFloat())
fun Int.toDp() = SizeUtils.px2dp(this.toFloat())
fun Float.toPx() = SizeUtils.dp2px( this)
fun Float.toSp()=SizeUtils.sp2px( this)

/**
 * 为 TextView 设置文本，并指定某一段文字的颜色
 * @param fullText 完整文本
 * @param target 需要变色的那一段文字（第一次出现）
 * @param color 颜色值（ColorInt 或 ColorRes）
 * @param ignoreCase 是否忽略大小写
 */
fun TextView.setColoredText(
    fullText: CharSequence,
    target: String,
    color: Int,
    ignoreCase: Boolean = false
) {
    if (target.isEmpty()) {
        text = fullText
        return
    }

    val spannable = SpannableStringBuilder(fullText)
    val textStr = fullText.toString()
    val searchStr = if (ignoreCase) target.lowercase() else target
    val sourceStr = if (ignoreCase) textStr.lowercase() else textStr

    val startIndex = sourceStr.indexOf(searchStr)
    if (startIndex != -1) {
        val endIndex = startIndex + target.length
        spannable.setSpan(
            ForegroundColorSpan(color),
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE // 不延伸影响新插入的字符
        )
    }
    text = spannable
}


/**
 * 新增方法：为 TextView 设置文本，并指定某一段文字的颜色（颜色为 String 类型，如 "#FF0000"）
 */
fun TextView.setColoredText(
    fullText: CharSequence,
    target: String,
    colorString: String,
    ignoreCase: Boolean = false
) {
    // 如果目标文本为空，或颜色字符串为空，直接显示原文本避免异常
    if (target.isEmpty()) {
        text = fullText
        return
    }

    val color = try {
        colorString.toColorInt() // 支持 "#RRGGBB"、"#AARRGGBB" 甚至 "red" 等
    } catch (e: IllegalArgumentException) {
        // 解析失败时，直接显示原文本（不染色），避免程序崩溃
        text = fullText
        return
    }

    // 复用上面 Int 版本的逻辑
    setColoredText(fullText, target, color, ignoreCase)
}

/**
 * 如果同一段文字要出现多次，全部变色（扩展重载）
 */
fun TextView.setColoredTextAllOccurrences(
    fullText: CharSequence,
    target: String,
    color: Int,
    ignoreCase: Boolean = false
) {
    if (target.isEmpty()) {
        text = fullText
        return
    }

    val spannable = SpannableStringBuilder(fullText)
    val textStr = fullText.toString()
    val searchStr = if (ignoreCase) target.lowercase() else target
    val sourceStr = if (ignoreCase) textStr.lowercase() else textStr

    var startIndex = sourceStr.indexOf(searchStr)
    while (startIndex != -1) {
        val endIndex = startIndex + target.length
        spannable.setSpan(
            ForegroundColorSpan(color),
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        startIndex = sourceStr.indexOf(searchStr, startIndex + target.length)
    }
    text = spannable
}
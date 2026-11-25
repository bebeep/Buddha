package com.fingertip.uilib.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * 支持手势锁定态的 ViewPager。
 *
 * 装扮页从佛灯弹窗进入时只让「灯具」一屏可见：把 [swipeEnabled] 置 false 后
 * 手指既不能从子 RecyclerView 传到 ViewPager 触发翻页，也不能直接拖 ViewPager；
 * 程序调用 setCurrentItem（初始定位、外部切分类）不受影响。
 */
class LockableViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    /** false = 禁止左右滑；子 View 的纵向滚动照常 */
    var swipeEnabled: Boolean = true

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!swipeEnabled) return false
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (_: Exception) {
            //ViewPager 在极端触摸序列下会抛IllegalArgument，沿用项目 TouchViewPager 的兜底
            false
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!swipeEnabled) return false
        return try {
            super.onTouchEvent(ev)
        } catch (_: Exception) {
            false
        }
    }
}

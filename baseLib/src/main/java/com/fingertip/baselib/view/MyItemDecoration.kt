package com.fingertip.baselib.view

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.event_bus.toPx

/**
 * recyclerview的分割线
 */
class MyItemDecoration(
    private var left: Int = 0,
    private var top: Int = 0,
    private var right: Int = 0,
    private var bottom: Int = 0
) : RecyclerView.ItemDecoration() {

    constructor(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float= 0f): this(left.toPx(), top.toPx(), right.toPx(), bottom.toPx())

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.set(left, top, right, bottom)
    }
}
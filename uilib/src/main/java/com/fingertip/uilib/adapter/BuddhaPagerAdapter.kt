package com.fingertip.uilib.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.uilib.R

/**
 * 佛龛轮播适配器（ViewPager2）
 *
 * 无限循环：itemCount 放大 [LOOPS] 倍，position 对 items.size 取模还原真实佛像；
 * 初始定位到中间一圈（[HALF_LOOPS]），单次会话滑不到头。
 * 佛像图片加载由 [imageLoader] 注入（页面里用 Glide，磁盘缓存）。
 */
class BuddhaPagerAdapter : RecyclerView.Adapter<BuddhaPagerAdapter.Holder>() {

    companion object {
        private const val LOOPS = 500
        private const val HALF_LOOPS = LOOPS / 2
    }

    private var items: List<BuddhaConfig> = emptyList()

    /** 图片加载回调：传入展示 View 与佛像 */
    var imageLoader: ((ImageView, BuddhaConfig) -> Unit)? = null

    fun submit(list: List<BuddhaConfig>) {
        items = list
        notifyDataSetChanged()
    }

    /** 真实佛像数量 */
    fun size(): Int = items.size

    /** 把 ViewPager 的放大坐标还原成真实佛像 */
    fun buddhaAt(position: Int): BuddhaConfig? =
        if (items.isEmpty()) null else items[position % items.size]

    /** 某尊佛像在无限列表里的全局坐标；单尊时不放大直接用真实下标，找不到返回 -1 */
    fun globalPositionOf(buddhaId: Int): Int {
        val idx = items.indexOfFirst { it.id == buddhaId }
        if (idx < 0) return -1
        return if (items.size <= 1) idx else idx + items.size * HALF_LOOPS
    }

    class Holder(val iv: ImageView) : RecyclerView.ViewHolder(iv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val iv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_buddha_page, parent, false) as ImageView
        return Holder(iv)
    }

    override fun getItemCount(): Int = when {
        items.isEmpty() -> 0
        items.size == 1 -> 1
        else -> items.size * LOOPS
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        buddhaAt(position)?.let { imageLoader?.invoke(holder.iv, it) }
    }
}

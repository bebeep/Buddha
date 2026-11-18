package com.fingertip.uilib.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * 早课五步滑动页：礼佛 / 诵经 / 念佛 / 发愿 / 回向。
 *
 * 五页在构造时一次预创建好（以 ViewPager2 为 parent  inflate，拿到正确的
 * RecyclerView.LayoutParams），Fragment 直接拿各页 view 绑 ViewBinding。
 * 这样不依赖 ViewPager2 下一帧的布局时机，setAdapter 后立刻可取控件。
 */
class EarlyPracticePagerAdapter(parent: ViewGroup) :
    RecyclerView.Adapter<EarlyPracticePagerAdapter.PageVH>() {

    private val layouts = intArrayOf(
        com.fingertip.uilib.R.layout.item_ep_worship,
        com.fingertip.uilib.R.layout.item_ep_chant,
        com.fingertip.uilib.R.layout.item_ep_nianfo,
        com.fingertip.uilib.R.layout.item_ep_vow,
        com.fingertip.uilib.R.layout.item_ep_review
    )

    private val inflater = LayoutInflater.from(parent.context)
    private val pages: List<View> = layouts.map { inflater.inflate(it, parent, false) }

    /** 取已创建的页面根 view，配合 ItemEpXxxBinding.bind() 使用 */
    fun pageView(position: Int): View = pages[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH = PageVH(pages[viewType])

    override fun onBindViewHolder(holder: PageVH, position: Int) = Unit

    override fun getItemCount(): Int = pages.size

    override fun getItemViewType(position: Int): Int = position

    class PageVH(view: View) : RecyclerView.ViewHolder(view)
}

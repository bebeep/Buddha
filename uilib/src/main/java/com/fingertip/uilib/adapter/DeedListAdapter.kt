package com.fingertip.uilib.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fingertip.baselib.bean.DeedBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemDeedBinding

/**
 * 行善列表卡片 Adapter
 */
class DeedListAdapter(
    context: Context,
    private val onClick: (DeedBean) -> Unit
) : TopRcAdapter<DeedBean, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_deed

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemDeedBinding>()
        val item = get(position) ?: return

        binding.tvTitle.text = item.title
        binding.tvContent.text = item.content
        binding.tvAuthor.text =
            "${if (item.isOfficial == 1) "指尖佛官方" else (item.accountInfo?.userName ?: "佛友")} · ${item.createDate?.take(10).orEmpty()}"

        //标签：官方/急 + 分类
        renderTags(binding, item)
        binding.tvProgress.text = "已筹 ${item.donatedCoins}/${item.targetCoins} 功德币"
        binding.progress.max = item.targetCoins.coerceAtLeast(1)
        binding.progress.progress = item.donatedCoins.coerceAtMost(item.targetCoins)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    private fun renderTags(binding: ItemDeedBinding, item: DeedBean) {
        val container = binding.llTags
        container.removeAllViews()
        if (item.isOfficial == 1) addTag(container, "官", Color.parseColor("#C0392B"))
        if (item.isUrgent == 1) addTag(container, "急", Color.parseColor("#E67E22"))
        item.category?.takeIf { it.isNotBlank() }?.let { addTag(container, it, Color.parseColor("#8A6B3A")) }
    }

    private fun addTag(container: LinearLayout, text: String, color: Int) {
        val tv = TextView(context)
        tv.text = text
        tv.textSize = 11f
        tv.setTextColor(color)
        val dp = (context.resources.displayMetrics.density * 4).toInt()
        tv.setPadding(dp * 2, dp / 2, dp * 2, dp / 2)
        tv.setBackgroundResource(R.drawable.bg_wish_tag)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = dp * 2
        container.addView(tv, lp)
    }
}

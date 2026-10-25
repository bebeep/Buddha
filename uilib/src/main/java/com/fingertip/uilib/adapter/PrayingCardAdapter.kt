package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.PrayingBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemPrayingCardBinding
import kotlin.math.abs

/**
 * 福卡墙卡片。每张卡带一点随机倾斜和错位，模拟设计稿的悬浮福卡效果
 */
class PrayingCardAdapter(
    context: Context,
    private val onClick: (PrayingBean) -> Unit
) : TopRcAdapter<PrayingBean, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_praying_card

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemPrayingCardBinding>()
        val item = get(position) ?: return

        binding.tvContent.text = item.content

        //按id做伪随机的小角度倾斜 + 水平错位
        val seed = abs(item.id * 31 + position * 7)
        binding.card.rotation = ((seed % 9) - 4f)
        binding.card.translationX = (seed % 3) * 4f

        //交替两种底色
        binding.card.setBackgroundResource(
            if (seed % 2 == 0) R.drawable.bg_praying_card else R.drawable.bg_praying_card_alt
        )

        binding.tvBlessCount.text = "${item.blessCount}人已祝福"
        binding.tvBlessCount.visibility =
            if (item.blessCount > 0) View.VISIBLE else View.INVISIBLE
        binding.tvCoverType.visibility =
            if (item.coverType > 0) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }
}

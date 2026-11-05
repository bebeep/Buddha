package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.MyDressItem
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemMyDressBinding

/**
 * 我的装扮网格 Adapter（2 列，卡片与商城一致）
 *
 * 卡片只负责展示所在分类的持有项，分类过滤在页面做（切 chip 只换列表，不重建 adapter）。
 * 佩戴位是每类一个，服务端 dress/wear 直接覆盖该类槽位，这里切完以服务端回包为准刷新。
 *
 * 动作按钮：
 *   佩戴中 →「卸下」，金色实底，一点即卸；
 *   未佩戴 →「佩戴」，金色描边。
 */
class MyDressAdapter(
    context: Context,
    private val onClick: (MyDressItem) -> Unit
) : TopRcAdapter<MyDressItem, TopRcAdapter.TopRcViewHolder>(context) {

    //当前佩戴中的装扮 id
    var wearingIds: Set<Int> = emptySet()

    override fun initLayoutId(viewType: Int): Int = R.layout.item_my_dress

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemMyDressBinding>()
        val item = get(position) ?: return

        //没有预览图时用名称首字当占位，与商城商品卡一致
        binding.tvPreviewChar.text = item.dressName?.take(1) ?: "装"
        binding.tvName.text = item.dressName

        //副标题：品类 · 来源（功德币购买 / 修持奖励 / 活动赠送）
        val typeName = ShopDressConfig.TYPE_NAMES.getOrNull(item.dressType).orEmpty()
        binding.tvSub.text = "$typeName · ${sourceText(item.source)}"

        val wearing = item.dressId in wearingIds
        binding.tvAction.setText(if (wearing) R.string.shop_action_take_off else R.string.shop_action_wear)
        binding.tvAction.setTextColor(
            context.resources.getColor(
                if (wearing) R.color.shop_text_on_gold else R.color.shop_gold
            )
        )
        binding.tvAction.setBackgroundResource(
            if (wearing) R.drawable.bg_shop_chip_on else R.drawable.bg_home_pill_outline
        )

        holder.itemView.setOnClickListener { onClick(item) }
    }

    private fun sourceText(source: Int): String = when (source) {
        1 -> "购买获得"
        2 -> "功德币购买"
        3 -> "修持奖励"
        else -> "已拥有"
    }
}

package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.MyDressItem
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemMyDressBinding

/**
 * 我的装扮列表 Adapter
 *
 * 一行一件，右侧动作在「佩戴 / 卸下」之间切。
 * 佩戴位是每类一个，这里只负责展示当前状态，切换逻辑交给服务端（dress/wear 直接覆盖该类的槽位）。
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

        binding.tvPreviewChar.text = item.dressName?.take(1) ?: "装"
        binding.tvName.text = item.dressName

        //副标题：品类 + 来源（IAP 购买 / 早课奖励 / 运营赠送）
        val typeName = ShopDressConfig.TYPE_NAMES.getOrNull(item.dressType).orEmpty()
        binding.tvSub.text = "$typeName · ${sourceText(item.source)}"

        val wearing = item.dressId in wearingIds
        binding.tvAction.setText(if (wearing) R.string.shop_action_take_off else R.string.shop_action_wear)
        binding.tvAction.setTextColor(
            context.resources.getColor(
                if (wearing) R.color.shop_text_disabled else R.color.shop_gold
            )
        )
        binding.tvAction.setBackgroundResource(
            if (wearing) R.drawable.bg_home_inner else R.drawable.bg_home_pill_outline
        )

        holder.itemView.setOnClickListener { onClick(item) }
    }

    private fun sourceText(source: Int): String = when (source) {
        1 -> "购买获得"
        2 -> "修持奖励"
        3 -> "活动赠送"
        else -> "已拥有"
    }
}

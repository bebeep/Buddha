package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemShopDressBinding

/**
 * 商城商品网格 Adapter
 *
 * 商品卡的状态判断全在 [ownedIds]/[wearingIds] 上，数据由页面从 dress/getMy 拿到后塞进来，
 * adapter 自己不请求网络——切换分类时只需要换列表 + 换这两个集合，不用重建 adapter。
 */
class ShopDressAdapter(
    context: Context,
    private val onClick: (ShopDressConfig) -> Unit
) : TopRcAdapter<ShopDressConfig, TopRcAdapter.TopRcViewHolder>(context) {

    //已拥有的装扮 id（灯油是消耗品，不会出现在这里）
    var ownedIds: Set<Int> = emptySet()

    //当前佩戴中的装扮 id
    var wearingIds: Set<Int> = emptySet()

    override fun initLayoutId(viewType: Int): Int = R.layout.item_shop_dress

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemShopDressBinding>()
        val item = get(position) ?: return

        //没有预览图时用商品名首字当占位，比空白块体面
        binding.tvPreviewChar.text = item.dressName?.take(1) ?: "装"
        binding.tvName.text = item.dressName
        binding.tvDesc.text = item.dressDesc
        binding.tvDuration.text = item.durationText()
        binding.tvPrice.text = item.priceText()

        val isWearing = item.id in wearingIds
        val isOwned = item.id in ownedIds
        when {
            //灯油：消耗品，买一次点一次灯，没有拥有态
            item.isOil() -> renderAction(binding, R.string.shop_action_buy, false)
            isWearing -> renderAction(binding, R.string.shop_action_in_use, true)
            isOwned -> renderAction(binding, R.string.shop_action_wear, false)
            else -> renderAction(binding, R.string.shop_action_buy, false)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    /**
     * @param used true 表示「使用中」，置灰不可点
     */
    private fun renderAction(binding: ItemShopDressBinding, textRes: Int, used: Boolean) {
        binding.tvAction.setText(textRes)
        binding.tvAction.setTextColor(
            if (used) {
                context.resources.getColor(R.color.shop_text_disabled)
            } else {
                context.resources.getColor(R.color.shop_gold)
            }
        )
        binding.tvAction.setBackgroundResource(
            if (used) R.drawable.bg_home_inner else R.drawable.bg_home_pill_outline
        )
        binding.tvAction.isEnabled = !used
    }
}

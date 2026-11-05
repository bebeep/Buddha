package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.CoinPackData
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.databinding.ItemCoinPackBinding

/**
 * 充值档位网格（3 列）
 *
 * 档位完全由服务端下发，这里只负责渲染；点卡片回调给页面去建单+拉起支付。
 */
class CoinPackAdapter(
    context: Context,
    private val onClick: (CoinPackData) -> Unit
) : TopRcAdapter<CoinPackData, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = com.fingertip.uilib.R.layout.item_coin_pack

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemCoinPackBinding>()
        val item = get(position) ?: return

        binding.tvCoin.text = item.totalCoin.toString()
        //无赠送时留空串而不是 GONE：卡片用 minHeight 撑住赠送行，同行卡片才能等高
        binding.tvExtra.text = if (item.extraCount > 0) "含赠送 ${item.extraCount}" else ""
        binding.tvPrice.text = item.priceText()

        holder.itemView.setOnClickListener { onClick(item) }
    }
}

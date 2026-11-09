package com.fingertip.uilib.adapter

import android.content.Context
import android.graphics.Paint
import android.view.View
import com.fingertip.baselib.bean.CoinPackData
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemCoinPackBinding

/**
 * 充值档位网格（3 列）
 *
 * 档位顺序与内容完全由服务端下发（首充档在前、组内价格升序），这里只负责渲染；
 * 点卡片回调给页面去建单+拉起支付。
 *
 * 首充优惠档（productType=2）右上角挂金色「首充」角标。
 * 资格用过之后服务端会整条不下发，页面层也做了一层过滤，正常不会出现「已使用」灰卡；
 * 这里保留已使用分支只作兜底（万一服务端仍在旧版本）。
 */
class CoinPackAdapter(
    context: Context,
    private val onClick: (CoinPackData) -> Unit
) : TopRcAdapter<CoinPackData, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_coin_pack

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemCoinPackBinding>()
        val item = get(position) ?: return

        binding.tvCoin.text = item.totalCoin.toString()
        //无赠送时留空串而不是 GONE：卡片用 minHeight 撑住赠送行，同行卡片才能等高
        binding.tvExtra.text = if (item.extraCount > 0) "含赠送 ${item.extraCount}" else ""
        binding.tvPrice.text = item.priceText()

        //划线原价：后台配了 oriPrice 且高于售价才显示，没配就不造假原价
        binding.tvOriPrice.visibility = if (item.showOriPrice) View.VISIBLE else View.GONE
        if (item.showOriPrice) {
            binding.tvOriPrice.text = item.oriPriceText()
            binding.tvOriPrice.paintFlags =
                binding.tvOriPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        //首充角标
        if (item.isFirstCharge) {
            binding.tvBadge.visibility = View.VISIBLE
            binding.tvBadge.setText(
                if (item.firstChargeAvailable) R.string.recharge_first_charge_badge
                else R.string.recharge_first_charge_used_badge
            )
            binding.tvBadge.setBackgroundResource(
                if (item.firstChargeAvailable) R.drawable.bg_first_charge_badge
                else R.drawable.bg_first_charge_badge_used
            )
            binding.llCard.alpha = if (item.firstChargeAvailable) 1f else 0.45f
        } else {
            binding.tvBadge.visibility = View.GONE
            binding.llCard.alpha = 1f
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }
}

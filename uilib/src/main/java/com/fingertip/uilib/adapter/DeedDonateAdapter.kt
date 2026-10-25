package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.DeedDonateBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemDeedDonateBinding

/**
 * 行善捐赠记录 Adapter
 */
class DeedDonateAdapter(context: Context) :
    TopRcAdapter<DeedDonateBean, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_deed_donate

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemDeedDonateBinding>()
        val item = get(position) ?: return

        binding.tvName.text = item.accountInfo?.userName ?: "佛友"
        binding.tvDate.text = " · ${item.createDate?.take(10).orEmpty()}"
        binding.tvMessage.text = if (item.message.isNullOrBlank()) "默默献上功德" else item.message
        binding.tvCoins.text = "x${item.coins}"
    }
}

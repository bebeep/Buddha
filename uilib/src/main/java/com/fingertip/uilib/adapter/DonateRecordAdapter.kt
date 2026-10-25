package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.DonateRecordBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemDonateRecordBinding

/**
 * 功德捐赠记录 Adapter
 */
class DonateRecordAdapter(context: Context) :
    TopRcAdapter<DonateRecordBean, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_donate_record

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemDonateRecordBinding>()
        val item = get(position) ?: return

        binding.tvBuddhaName.text = item.buddhaName ?: "功德捐赠"
        binding.tvDate.text = item.createDate ?: ""
        binding.tvCoin.text = "-${item.coin}"
    }
}

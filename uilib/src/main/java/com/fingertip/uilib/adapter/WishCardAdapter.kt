package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.WishRecordBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemWishCardBinding

/**
 * 许愿树愿望卡 Adapter
 */
class WishCardAdapter(
    context: Context,
    private val onItemClick: (WishRecordBean) -> Unit
) : TopRcAdapter<WishRecordBean, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_wish_card

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemWishCardBinding>()
        val item = get(position) ?: return

        binding.tvWishContent.text = item.wishContent.orEmpty()
        binding.tvWishInfo.text = if (item.blessUserCount > 0) "${item.blessUserCount} 人已祝愿" else "静待有缘人祝愿"

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }
}

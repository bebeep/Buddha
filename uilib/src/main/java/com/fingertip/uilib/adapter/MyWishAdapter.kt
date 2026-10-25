package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.WishRecordBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemMyWishBinding

/**
 * 我的愿望 Adapter。点删除回调 onDelete，点整条回调 onShowBless 看收到的祝福
 */
class MyWishAdapter(
    context: Context,
    private val onShowBless: (WishRecordBean) -> Unit,
    private val onDelete: (WishRecordBean) -> Unit
) : TopRcAdapter<WishRecordBean, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_my_wish

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemMyWishBinding>()
        val item = get(position) ?: return

        binding.tvWishContent.text = item.wishContent.orEmpty()
        binding.tvWishTime.text = item.createDate.orEmpty()
        binding.tvWishBlessCount.text = "${item.blessUserCount} 人已祝愿"

        binding.tvWishDelete.setOnClickListener { onDelete(item) }
        holder.itemView.setOnClickListener { onShowBless(item) }
    }
}

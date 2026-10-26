package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImgNoHolder
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemBuddhaPhotoBinding

/**
 * 佛像法相图册 Adapter（横滑）
 */
class BuddhaPhotoAdapter(
    context: Context,
    list: List<String>
) : TopRcAdapter<String, TopRcAdapter.TopRcViewHolder>(context) {

    init {
        initData(list)
    }

    override fun initLayoutId(viewType: Int): Int = R.layout.item_buddha_photo

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemBuddhaPhotoBinding>()
        val url = get(position) ?: return
        binding.ivPhoto.loadImgNoHolder(url)
    }
}

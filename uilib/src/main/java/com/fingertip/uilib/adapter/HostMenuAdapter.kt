package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.HostMenu
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImgNoHolder
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemHostMenuBinding

class HostMenuAdapter(context: Context, val onItemClick: (pos: Int) -> Unit) : TopRcAdapter<HostMenu, TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int): Int = R.layout.item_host_menu

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemHostMenuBinding>()
        get(position)?.let {
            binding.tvMenu.text = it.name
            binding.ivMenu.loadImgNoHolder(it.resId)
            binding.clParent.setBackgroundResource(it.bgResId)

            holder.itemView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(p0: View?) {
                    onItemClick(holder.adapterPosition)
                }
            })
        }
    }
}

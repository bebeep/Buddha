package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.HostMenu
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImgNoHolder
import com.fingertip.uilib.R
import kotlinx.android.synthetic.main.item_host_menu.view.*

class HostMenuAdapter(context: Context,val onItemClick:(pos:Int)->Unit) : TopRcAdapter<HostMenu, TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int): Int = R.layout.item_host_menu

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let {
            holder.itemView.tv_menu.text = it.name
            holder.itemView.iv_menu.loadImgNoHolder(it.resId)
            holder.itemView.cl_parent.setBackgroundResource(it.bgResId)

            holder.itemView.setOnClickListener(object : View.OnClickListener{
                override fun onClick(p0: View?) {
                    onItemClick(holder.adapterPosition)
                }
            })
        }
    }

}
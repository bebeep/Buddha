package com.fingertip.uilib.adapter

import android.content.Context
import android.graphics.Color
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.lzlz.toplib.extention.setHeight
import com.lzlz.toplib.extention.toPx
import kotlinx.android.synthetic.main.item_message_system.view.*

/**
 *
 */
class MessageSystemAdapter(context: Context, val onItemClick:(position:Int)->Unit):
    TopRcAdapter<String, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int) = R.layout.item_message_system

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let{
            holder.itemView.v_line_bottom.setBackgroundColor(Color.parseColor(if (position == mlist.size-1) "#00000000" else "#F2F4F6"))
            holder.itemView.v_line_bottom.setHeight(if (position == mlist.size-1) 80.toPx() else 0)

            holder.itemView.ll_system_parent.setOnClickListener { view-> onItemClick(position) }
            holder.itemView.ll_system_parent.setOnLongClickListener { view->
                onItemClick(position)
                return@setOnLongClickListener true
            }
        }

    }

}
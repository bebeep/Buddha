package com.fingertip.uilib.adapter

import com.fingertip.baselib.R as BaseR
import com.fingertip.baselib.top.TopApplication
import android.content.Context
import android.graphics.Color
import android.view.View
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.lzlz.toplib.extention.setHeight
import com.lzlz.toplib.extention.toPx

/**
 *
 */
class MessageSystemAdapter(context: Context, val onItemClick:(position:Int)->Unit):
    TopRcAdapter<String, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int) = R.layout.item_message_system

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let{
            holder.itemView.findViewById<View>(R.id.v_line_bottom).setBackgroundColor(if (position == mlist.size-1) TopApplication.instance.getColor(BaseR.color.transparent) else TopApplication.instance.getColor(BaseR.color.c_bg_card))
            holder.itemView.findViewById<View>(R.id.v_line_bottom).setHeight(if (position == mlist.size-1) 80.toPx() else 0)

            holder.itemView.findViewById<View>(R.id.ll_system_parent).setOnClickListener { view-> onItemClick(position) }
            holder.itemView.findViewById<View>(R.id.ll_system_parent).setOnLongClickListener { view->
                onItemClick(position)
                return@setOnLongClickListener true
            }
        }

    }

}
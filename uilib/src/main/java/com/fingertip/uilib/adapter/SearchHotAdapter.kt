package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R

/**
 * 搜索-热门搜索
 */
class SearchHotAdapter(context: Context, val onItemClick:(position:Int)->Unit):
    TopRcAdapter<String, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int) = R.layout.item_search_hot

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let{

            holder.itemView.findViewById<android.widget.TextView>(R.id.tv_search_hot_item).text = it


            holder.itemView.findViewById<android.widget.TextView>(R.id.tv_search_hot_item).setOnClickListener {  onItemClick(position) }

        }

    }

}
package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import kotlinx.android.synthetic.main.item_search_hot.view.*

/**
 * 搜索-热门搜索
 */
class SearchHotAdapter(context: Context, val onItemClick:(position:Int)->Unit):
    TopRcAdapter<String, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int) = R.layout.item_search_hot

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let{

            holder.itemView.tv_search_hot_item.text = it


            holder.itemView.tv_search_hot_item.setOnClickListener {  onItemClick(position) }

        }

    }

}
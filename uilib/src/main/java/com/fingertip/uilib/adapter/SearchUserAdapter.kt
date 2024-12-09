package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import kotlinx.android.synthetic.main.item_search_user.view.*

class SearchUserAdapter(context: Context, val onItemClick:(entity:String)->Unit) : TopRcAdapter<String, TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int): Int = R.layout.item_search_user


    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {

        get(position)?.let {

        }
    }

}
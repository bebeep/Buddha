package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.MomentBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R

class MomentAdapter(context: Context) : TopRcAdapter<MomentBean, TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int): Int = R.layout.item_moment

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {

    }

}
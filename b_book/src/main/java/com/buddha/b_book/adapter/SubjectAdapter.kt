package com.buddha.b_book.adapter

import android.content.Context
import com.buddha.b_book.R
import com.fingertip.baselib.top.TopRcAdapter

/**
 * 专题
 */
class SubjectAdapter(context: Context,val onItemClick:(pos:Int)->Unit):TopRcAdapter<String,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_subject

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let {

            holder.itemView.setOnClickListener {
                onItemClick(position)
            }
        }
    }
}
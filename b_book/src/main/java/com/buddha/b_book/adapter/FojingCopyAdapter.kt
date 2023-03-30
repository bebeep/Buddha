package com.buddha.b_book.adapter

import android.content.Context
import android.view.View
import com.buddha.b_book.R
import com.fingertip.baselib.top.TopRcAdapter
import kotlinx.android.synthetic.main.item_fojing_copy.view.*

/**
 * 佛经-抄经
 */
class FojingCopyAdapter(context: Context,val column:Int):TopRcAdapter<String,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_fojing_copy

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let {
            holder.itemView.v_line_right.visibility = if (position%column == column-1 || position == data().size-1 ) View.VISIBLE else View.GONE
        }
    }
}
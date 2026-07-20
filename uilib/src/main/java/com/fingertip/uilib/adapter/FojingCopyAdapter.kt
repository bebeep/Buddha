package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.uilib.R
import com.fingertip.baselib.top.TopRcAdapter

/**
 * 佛经-抄经
 */
class FojingCopyAdapter(context: Context,val column:Int):TopRcAdapter<String,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_fojing_copy

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let {
            holder.itemView.findViewById<View>(R.id.v_line_right).visibility = if (position%column == column-1 || position == data().size-1 ) View.VISIBLE else View.GONE
        }
    }
}
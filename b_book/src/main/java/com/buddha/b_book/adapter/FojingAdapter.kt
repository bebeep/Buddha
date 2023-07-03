package com.buddha.b_book.adapter

import android.content.Context
import com.buddha.b_book.R
import com.buddha.b_book.fragment.FojingDetailsFragment
import com.fingertip.baselib.top.TopRcAdapter
import com.weikaiyun.fragmentation.SupportActivity

/**
 * 佛经
 */
class FojingAdapter(context: Context):TopRcAdapter<String,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_fojing

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let {
            holder.itemView.setOnClickListener {
                (context as? SupportActivity)?.start(FojingDetailsFragment())
            }
        }
    }
}
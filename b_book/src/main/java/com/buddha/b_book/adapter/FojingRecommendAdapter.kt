package com.buddha.b_book.adapter

import android.content.Context
import com.buddha.b_book.R
import com.fingertip.baselib.top.TopRcAdapter

/**
 * 大师推荐
 */
class FojingRecommendAdapter(context: Context):TopRcAdapter<String,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_book_recommend

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let {

        }
    }
}
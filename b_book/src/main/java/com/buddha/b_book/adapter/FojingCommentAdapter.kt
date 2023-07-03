package com.buddha.b_book.adapter

import android.content.Context
import com.buddha.b_book.R
import com.fingertip.baselib.top.TopRcAdapter

/**
 * 经书-佛友评论
 */
class FojingCommentAdapter(context: Context):TopRcAdapter<String,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_book_comment

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let {

        }
    }
}
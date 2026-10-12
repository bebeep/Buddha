package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.BookComment
import com.fingertip.uilib.R
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.databinding.ItemBookCommentBinding

/**
 * 经书-佛友评论
 */
class FojingCommentAdapter(context: Context):TopRcAdapter<BookComment,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_book_comment

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemBookCommentBinding>()
        get(position)?.let { comment ->
            binding.ivHead.loadImg(comment.avatar)
            binding.tvCommentName.text = comment.userName
            binding.tvCommentLevel.text = "level ${comment.userLevel}"
            binding.ivCommentLike.isSelected = comment.isLiked
            binding.tvCommentLike.text = "${comment.likeCount}"
            binding.tvCommentContent.text = comment.textContent
        }
    }
}
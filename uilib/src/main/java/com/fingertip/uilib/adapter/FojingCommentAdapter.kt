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
class FojingCommentAdapter(context: Context,val onItemClick: (viewId: Int, entity: BookComment) -> Unit):TopRcAdapter<BookComment,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_book_comment

    companion object {
        /** 点赞局部刷新payload */
        const val PAYLOAD_LIKE = "payload_like"
    }

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemBookCommentBinding>()
        get(position)?.let { comment ->
            binding.ivHead.loadImg(comment.avatar)
            binding.tvCommentName.text = comment.userName
            binding.tvCommentLevel.text = "level ${comment.userLevel}"
            binding.ivCommentLike.isSelected = comment.isLiked
            binding.tvCommentLike.text = "${comment.likeCount}"
            binding.tvCommentContent.text = comment.textContent

            binding.llLike.setOnClickListener { v->
                onItemClick.invoke(v.id,comment)
            }
        }
    }

    /**
     * 局部刷新：payload 命中时只更新点赞相关控件，跳过头像加载，避免占位图闪烁
     */
    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_LIKE)) {
            val binding = holder.getBinding<ItemBookCommentBinding>()
            get(position)?.let { bindLike(binding, it) }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    /** 绑定点赞状态与数量 */
    private fun bindLike(binding: ItemBookCommentBinding, comment: BookComment) {
        binding.ivCommentLike.isSelected = comment.isLiked
        binding.tvCommentLike.text = "${comment.likeCount}"
    }
}
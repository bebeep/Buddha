package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.BookComment
import com.fingertip.uilib.R
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.databinding.ItemBookRecommendBinding

/**
 * 大师推荐
 */
class FojingRecommendAdapter(context: Context,
                             val onItemClick: (viewId: Int, entity: BookComment) -> Unit,
                             val onItemLongClick: ((viewId: Int, entity: BookComment) -> Unit)):TopRcAdapter<BookComment,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_book_recommend

    companion object {
        /** 点赞局部刷新payload */
        const val PAYLOAD_LIKE = "payload_like"
    }

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemBookRecommendBinding>()
        get(position)?.let { comment ->
            binding.ivHead.loadImg(comment.avatar)
            binding.tvMasterName.text = comment.userName
            binding.tvMasterIntro.text = "大师简介"
            binding.ivMasterLike.isSelected = comment.isLiked
            binding.tvMasterLike.text = "${comment.likeCount}"
            binding.tvMasterContent.text = comment.textContent

            binding.llLike.setOnClickListener { v->
                onItemClick.invoke(v.id,comment)
            }
            binding.tvMasterContent.setOnLongClickListener { v->
                onItemLongClick(v.id,comment)
                return@setOnLongClickListener true
            }
        }
    }


    /**
     * 局部刷新：payload 命中时只更新点赞相关控件，跳过头像加载，避免占位图闪烁
     */
    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_LIKE)) {
            val binding = holder.getBinding<ItemBookRecommendBinding>()
            get(position)?.let { bindLike(binding, it) }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    /** 绑定点赞状态与数量 */
    private fun bindLike(binding: ItemBookRecommendBinding, comment: BookComment) {
        binding.ivMasterLike.isSelected = comment.isLiked
        binding.tvMasterLike.text = "${comment.likeCount}"
    }
}
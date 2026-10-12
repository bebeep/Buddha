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
class FojingRecommendAdapter(context: Context):TopRcAdapter<BookComment,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_book_recommend

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemBookRecommendBinding>()
        get(position)?.let { comment ->
            binding.ivHead.loadImg(comment.avatar)
            binding.tvMasterName.text = comment.userName
            binding.tvMasterIntro.text = "大师简介"
            binding.ivMasterLike.isSelected = comment.isLiked
            binding.tvMasterLike.text = "${comment.likeCount}"
            binding.tvMasterContent.text = comment.textContent
        }
    }
}
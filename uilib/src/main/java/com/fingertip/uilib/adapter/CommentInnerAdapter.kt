package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.TimeUtil
import com.fingertip.baselib.util.clearFirstLine
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemCommentBinding
import com.fingertip.uilib.databinding.ItemCommentInnerBinding
import com.lzlz.toplib.extention.setColoredText

/**
 * 评论
 */
class CommentInnerAdapter(context: Context, val onItemClick:(position:Int,longClickPosition:Int, viewId:Int)->Unit):
    TopRcAdapter<CommentEntity, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int) = R.layout.item_comment_inner

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemCommentInnerBinding>()
        get(position)?.let{

            binding.ivInnerHead.loadHead(it.senderAvatar)
            binding.tvInnerNickname.text = it.senderNickName
            binding.tvInnerComment.text = it.textContent.clearFirstLine()
            binding.tvInnerDate.text = TimeUtil.dateFormateTime(it.commentDate)
            binding.tvInnerLike.text = it.likeCount.toString()
            binding.tvInnerLike.isSelected = it.isLiked

            if (!it.replyNickName.isNullOrEmpty())
            {
                binding.tvInnerComment.setColoredText("回复 ${it.replyNickName}: ${it.textContent.clearFirstLine()}", "${it.replyNickName}", "#33000000")

            }

            binding.ivInnerHead.setOnClickListener { view ->  onItemClick(holder.bindingAdapterPosition,-1,view.id) }
            binding.tvInnerNickname.setOnClickListener { view ->  onItemClick(holder.bindingAdapterPosition,-1,view.id) }
            binding.tvInnerLike.setOnClickListener { view ->
                onItemClick(holder.bindingAdapterPosition,-1,view.id)
                log("tvInnerLike", "${position}->${binding.tvInnerLike.isSelected} ${it.likeCount}")
                if (binding.tvInnerLike.isSelected)
                    it.likeCount--
                else
                    it.likeCount++
                it.isLiked = !it.isLiked
                binding.tvInnerLike.isSelected = it.isLiked
                binding.tvInnerLike.text = "${it.likeCount}"
            }

            binding.clInnerParent.setOnClickListener { view-> onItemClick(holder.bindingAdapterPosition,-1,view.id) }
            binding.clInnerParent.setOnLongClickListener { view->
                onItemClick(holder.bindingAdapterPosition,holder.bindingAdapterPosition,view.id)
                return@setOnLongClickListener true
            }
        }

    }


}
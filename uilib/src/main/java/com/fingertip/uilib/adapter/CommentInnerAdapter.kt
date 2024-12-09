package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.TimeUtil
import com.fingertip.baselib.util.clearFirstLine
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import kotlinx.android.synthetic.main.item_comment_inner.view.*

/**
 * 评论
 */
class CommentInnerAdapter(context: Context, val onItemClick:(position:Int,longClickPosition:Int, viewId:Int)->Unit):
    TopRcAdapter<CommentEntity, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int) = R.layout.item_comment_inner

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let{

            holder.itemView.iv_inner_head.loadHead(it.senderAvatar)
            holder.itemView.tv_inner_nickname.text = it.senderNickname
            holder.itemView.tv_inner_comment.text = it.content.clearFirstLine()
            holder.itemView.tv_inner_date.text = TimeUtil.dateFormatTime(TimeUtil.getTimeUtcDate(it.commentTime))

            holder.itemView.iv_inner_head.setOnClickListener { _->   }
            holder.itemView.tv_inner_nickname.setOnClickListener { _->   }


            holder.itemView.cl_inner_parent.setOnClickListener { view-> onItemClick(position,-1,view.id) }
            holder.itemView.cl_inner_parent.setOnLongClickListener { view->
                onItemClick(position,position,view.id)
                return@setOnLongClickListener true
            }
        }

    }

}
package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.TimeUtil
import com.fingertip.baselib.util.clearFirstLine
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R

/**
 * 评论
 */
class CommentInnerAdapter(context: Context, val onItemClick:(position:Int,longClickPosition:Int, viewId:Int)->Unit):
    TopRcAdapter<CommentEntity, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int) = R.layout.item_comment_inner

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let{

            holder.itemView.findViewById<android.widget.ImageView>(R.id.iv_inner_head).loadHead(it.senderAvatar)
            holder.itemView.findViewById<android.widget.TextView>(R.id.tv_inner_nickname).text = it.senderNickname
            holder.itemView.findViewById<android.widget.TextView>(R.id.tv_inner_comment).text = it.content.clearFirstLine()
            holder.itemView.findViewById<android.widget.TextView>(R.id.tv_inner_date).text = TimeUtil.dateFormatTime(TimeUtil.getTimeUtcDate(it.commentTime))

            holder.itemView.findViewById<android.widget.ImageView>(R.id.iv_inner_head).setOnClickListener { _->   }
            holder.itemView.findViewById<android.widget.TextView>(R.id.tv_inner_nickname).setOnClickListener { _->   }


            holder.itemView.findViewById<View>(R.id.cl_inner_parent).setOnClickListener { view-> onItemClick(position,-1,view.id) }
            holder.itemView.findViewById<View>(R.id.cl_inner_parent).setOnLongClickListener { view->
                onItemClick(position,position,view.id)
                return@setOnLongClickListener true
            }
        }

    }

}
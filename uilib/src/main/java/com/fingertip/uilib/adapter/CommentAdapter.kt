package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.TimeUtil
import com.fingertip.baselib.util.clearFirstLine
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import kotlinx.android.synthetic.main.item_comment.view.*
import java.util.*
import kotlin.collections.HashMap

/**
 * 评论
 */
class CommentAdapter(context: Context, val onItemClick:(position:Int,innerPosition:Int,longClickPosition:Int, viewId:Int)->Unit):
    TopRcAdapter<CommentEntity, TopRcAdapter.TopRcViewHolder>(context) {

    //显示的评论回复的条数
    val showCountMap = HashMap<String,Int>()

    override fun initLayoutId(viewType: Int) = R.layout.item_comment

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        get(position)?.let{
            if (showCountMap["COMMENT${it.commentId}"] == null)showCountMap["COMMENT${it.commentId}"] = 2
            var showCount = showCountMap["COMMENT${it.commentId}"]?:2
            holder.itemView.iv_head.loadImg(it.senderAvatar, width = 60, height = 60)
            holder.itemView.tv_nickname.text = it.senderNickname
            holder.itemView.tv_comment.text = it.content.clearFirstLine()
            holder.itemView.recyclerview.layoutManager = LinearLayoutManager(context)
            holder.itemView.tv_more.visibility = if (it.expandComments.size > showCount) View.VISIBLE else View.GONE


            val adapter = CommentInnerAdapter(context){innerPosition,longClickPosition, viewId ->
                onItemClick(position, innerPosition,longClickPosition, viewId)
            }
            holder.itemView.recyclerview.adapter = adapter
            if (it.expandComments.size > showCount) adapter.initData(it.expandComments.subList(0,showCount))
            else adapter.initData(it.expandComments)

            holder.itemView.iv_head.setOnClickListener { _->   }
            holder.itemView.tv_nickname.setOnClickListener { _->    }
            holder.itemView.cl_parent.setOnClickListener { view->onItemClick(position,-1,-1,view.id) }
            holder.itemView.cl_parent.setOnLongClickListener {view->
                onItemClick(position,-1,position,view.id)
                return@setOnLongClickListener true
            }
            holder.itemView.tv_more.setOnClickListener { _ ->
                if (showCount < it.expandComments.size){
                    showCount += 5
                    showCountMap["COMMENT${it.commentId}"] = showCount
                    if (showCount > it.expandComments.size) showCount = it.expandComments.size
                    adapter.initData(it.expandComments.subList(0,showCount))
                }
                holder.itemView.tv_more.visibility = if (it.expandComments.size > showCount) View.VISIBLE else View.GONE
            }
        }
    }
}
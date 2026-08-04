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
import com.fingertip.uilib.databinding.ItemCommentBinding
import com.lzlz.toplib.extention.gone
import com.lzlz.toplib.extention.invisible
import com.lzlz.toplib.extention.visible
import java.util.*
import kotlin.collections.HashMap

/**
 * 评论
 */
class CommentAdapter(context: Context, val onItemClick:(position:Int,innerPosition:Int,longClickPosition:Int, viewId:Int)->Unit):
    TopRcAdapter<CommentEntity, TopRcAdapter.TopRcViewHolder>(context) {

    val maxChildCommentCount = 3

    override fun initLayoutId(viewType: Int) = R.layout.item_comment

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemCommentBinding>()
        get(position)?.let{
            binding.tvMore.visibility = if ((it.childComment?.size?:0)>maxChildCommentCount) View.VISIBLE else View.GONE

            binding.ivHead.loadImg(it.senderAvatar, width = 60, height = 60)
            binding.tvLevel.text = "lv.${it.senderLevel}·${TimeUtil.dateFormateTime(it.commentDate)}"
            binding.tvNickname.text = it.senderNickName
            binding.tvComment.text = it.textContent.clearFirstLine()
            binding.tvMore.text = "查看更多${(it.childComment?.size?:0) - maxChildCommentCount}条回复"
            binding.tvFold.gone()

            it.childComment?.let { childComment ->
                binding.recyclerview.layoutManager = LinearLayoutManager(context)


                val adapter = CommentInnerAdapter(context){innerPosition,longClickPosition, viewId ->
                    onItemClick(position, innerPosition,longClickPosition, viewId)
                }
                binding.recyclerview.adapter = adapter
                if (childComment.size > 3)
                {
                    adapter.initData(childComment.subList(0,3))
                }
                else
                {
                    adapter.initData(childComment)
                }
            }

            binding.ivHead.setOnClickListener { view->onItemClick(position,-1,-1,view.id) }
            binding.tvNickname.setOnClickListener { view->onItemClick(position,-1,-1,view.id) }
            binding.clParent.setOnClickListener { view->onItemClick(position,-1,-1,view.id) }
            binding.clParent.setOnLongClickListener {view->
                onItemClick(position,-1,position,view.id)
                return@setOnLongClickListener true
            }
            binding.tvMore.setOnClickListener { view ->
                if ((it.childComment?.size ?: 0) >= it.childCommentCount){ //直接全部显示
                    val adapter = binding.recyclerview.adapter as CommentInnerAdapter
                    adapter.initData(it.childComment)
                    binding.tvMore.invisible()
                    binding.tvFold.visible()
                }
                else //还有额外评论,需要弹窗分页显示
                {
                    onItemClick(position,-1,-1,view.id)
                }
            }

            binding.tvFold.setOnClickListener { _ ->
                binding.tvFold.gone()
                binding.tvMore.visibility = if ((it.childComment?.size?:0)>maxChildCommentCount) View.VISIBLE else View.GONE
                val adapter = binding.recyclerview.adapter as CommentInnerAdapter
                it.childComment?.let { childComment->
                    if (childComment.size > 3)
                    {
                        adapter.initData(childComment.subList(0,3))
                    }
                    else
                    {
                        adapter.initData(childComment)
                    }
                }
            }
        }
    }
}
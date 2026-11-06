package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.TempleCommentBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemTempleCommentBinding

/**
 * 寺庙评论 Adapter
 */
class TempleCommentAdapter(
    context: Context,
    //头像/昵称点击：跳用户主页，不传就不响应
    val onUserClick: ((TempleCommentBean) -> Unit)? = null
) : TopRcAdapter<TempleCommentBean, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_temple_comment

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemTempleCommentBinding>()
        val item = get(position) ?: return

        binding.tvName.text = item.nickName ?: "佛友"
        com.fingertip.uilib.utils.LevelUi.bindTag(binding.ivLevelTag, item.userLevel)
        binding.tvLevel.text = com.fingertip.uilib.utils.LevelUi.tagTitle(item.userLevel)
        binding.tvDate.text = item.commentDate?.take(10).orEmpty()
        binding.tvContent.text = item.content
        binding.ivAvatar.loadHead(item.avatar)

        onUserClick?.let { cb ->
            binding.ivAvatar.setOnClickListener { cb(item) }
            binding.tvName.setOnClickListener { cb(item) }
        }
    }
}

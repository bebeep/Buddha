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
class TempleCommentAdapter(context: Context) :
    TopRcAdapter<TempleCommentBean, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_temple_comment

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemTempleCommentBinding>()
        val item = get(position) ?: return

        binding.tvName.text = item.nickName ?: "佛友"
        binding.tvLevel.text = "lv${item.userLevel}"
        binding.tvDate.text = item.commentDate?.take(10).orEmpty()
        binding.tvContent.text = item.content
        binding.ivAvatar.loadHead(item.avatar)
    }
}

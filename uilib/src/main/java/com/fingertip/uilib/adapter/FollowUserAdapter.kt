package com.fingertip.uilib.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.fingertip.baselib.bean.FollowUserEntity
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.top.TopRcAdapter.TopRcViewHolder
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.databinding.ItemFollowUserBinding
import com.fingertip.uilib.utils.LevelUi

/**
 * 粉丝 / 关注列表用户卡片。
 * 专用实体 + 专用布局，与搜索页的 SearchUserAdapter 完全解耦：
 * 关系列表要改字段或样式，不影响搜索，反之亦然。
 */
class FollowUserAdapter(
    context: Context,
    val onItemClick: (entity: FollowUserEntity) -> Unit
) : TopRcAdapter<FollowUserEntity, FollowUserAdapter.UserVH>(context) {

    override fun initLayoutId(viewType: Int): Int = com.fingertip.uilib.R.layout.item_follow_user

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVH {
        val binding = ItemFollowUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserVH(binding)
    }

    override fun onBindViewHolder(holder: UserVH, position: Int) {
        val entity = get(position) ?: return
        val binding = holder.binding
        binding.ivHead.loadHead(entity.avatar)
        binding.tvName.text = entity.userName.orEmpty()
        binding.tvLevel.text = if (entity.userLevel > 0) {
            "Lv.${entity.userLevel} · ${LevelUi.tagTitle(entity.userLevel)}"
        } else ""
        binding.tvMood.text = entity.mood.orEmpty()
        holder.itemView.setOnClickListener { onItemClick(entity) }
    }

    class UserVH(val binding: ItemFollowUserBinding) : TopRcViewHolder(binding.root)
}

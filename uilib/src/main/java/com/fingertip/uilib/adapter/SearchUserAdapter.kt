package com.fingertip.uilib.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fingertip.baselib.bean.SearchUserEntity
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.top.TopRcAdapter.TopRcViewHolder
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemSearchUserBinding
import com.fingertip.uilib.utils.LevelUi
import com.lzlz.toplib.extention.setColoredTextAllOccurrences
import com.fingertip.baselib.R as BaseR

/**
 * 搜索结果里的佛友卡片。
 * 只展示基本信息（头像/昵称/等级/展示 ID），命中关键词染成亮金。
 */
class SearchUserAdapter(
    context: Context,
    val onItemClick: (entity: SearchUserEntity) -> Unit
) : TopRcAdapter<SearchUserEntity, SearchUserAdapter.UserVH>(context) {

    /** 搜索页每次开搜前更新，holder 复用时也会按最新关键词渲染 */
    var keyword: String = ""

    private val highlightColor: Int by lazy {
        TopApplication.instance.getColor(BaseR.color.c_gold_light)
    }

    override fun initLayoutId(viewType: Int): Int = R.layout.item_search_user

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVH {
        val binding = ItemSearchUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserVH(binding)
    }

    override fun onBindViewHolder(holder: UserVH, position: Int) {
        val entity = get(position) ?: return
        val binding = holder.binding
        binding.ivHead.loadHead(entity.avatar)
        binding.tvName.setColoredTextAllOccurrences(
            entity.userName.orEmpty(), keyword, highlightColor, true
        )
        binding.tvLevel.text = if (entity.userLevel > 0) {
            "Lv.${entity.userLevel} · ${LevelUi.tagTitle(entity.userLevel)}"
        } else ""
        binding.tvTimeLocate.setColoredTextAllOccurrences(
            context.getString(R.string.search_user_id, entity.displayAccountId.orEmpty()),
            keyword, highlightColor, true
        )
        //需求只要基本信息，关注操作仍进主页完成
        binding.tvFollow.visibility = View.GONE
        holder.itemView.setOnClickListener { onItemClick(entity) }
    }

    class UserVH(val binding: ItemSearchUserBinding) : TopRcViewHolder(binding.root)
}

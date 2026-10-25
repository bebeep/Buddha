package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.TempleBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemTempleBinding

/**
 * 寺庙列表 Adapter
 */
class TempleListAdapter(
    context: Context,
    private val onClick: (TempleBean) -> Unit
) : TopRcAdapter<TempleBean, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_temple

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemTempleBinding>()
        val item = get(position) ?: return

        binding.tvName.text = item.templeName
        binding.tvTitle.text = item.templeTitle
        binding.tvIntro.text = item.templeIntro
        binding.tvAddr.text = item.templeAddr
        binding.tvDistance.text = item.distanceText()
        binding.tvDistance.visibility =
            if (item.distanceText().isEmpty()) View.GONE else View.VISIBLE
        binding.tvUp.text = "推荐 ${item.recommendUp}"
        binding.tvDown.text = "不推荐 ${item.recommendDown}"
        binding.tvComment.text = "评论 ${item.commentCount}"

        val cover = item.coverList().firstOrNull()
        if (cover.isNullOrBlank()) {
            binding.ivCover.setImageResource(R.mipmap.icon_menu_gongfo)
        } else {
            binding.ivCover.loadImg(cover)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }
}

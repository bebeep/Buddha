package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.CollectData
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemCollectBinding

/**
 * 收藏列表
 *
 * 三类目标共用一个列表：佛经 / 感悟 / 寺庙用类型标签区分，
 * 标题和封面都来自服务端 join 的结果，本地不按 id 反查。
 */
class CollectAdapter(
    context: Context,
    private val onClick: (CollectData) -> Unit,
    private val onCancel: (CollectData) -> Unit
) : TopRcAdapter<CollectData, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_collect

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemCollectBinding>()
        val item = get(position) ?: return

        //标题为空说明目标已被删除/下架，给个明确文案，别显示一条空白卡
        binding.tvTitle.text = item.targetTitle?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.collect_untitled)

        binding.tvType.text = when (item.targetType) {
            CollectData.TYPE_SUTRA -> context.getString(R.string.collect_tab_sutra)
            CollectData.TYPE_MOMENT -> context.getString(R.string.collect_tab_moment)
            CollectData.TYPE_TEMPLE -> context.getString(R.string.collect_tab_temple)
            else -> ""
        }

        binding.tvDate.text = item.createDate?.takeIf { it.length >= 10 }?.substring(0, 10) ?: ""

        binding.ivCover.loadImg(item.targetCover)

        binding.tvCancel.setOnClickListener { onCancel.invoke(item) }
        holder.itemView.setOnClickListener { onClick.invoke(item) }
    }
}

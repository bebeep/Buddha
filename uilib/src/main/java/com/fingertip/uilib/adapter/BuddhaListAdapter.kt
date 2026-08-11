package com.fingertip.uilib.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImgNoHolder
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemBuddhaBinding

/**
 * 佛像列表 Adapter（水平 RecyclerView）
 */
class BuddhaListAdapter(
    context: Context,
    private val onItemSelected: (position: Int) -> Unit
) : TopRcAdapter<BuddhaConfig, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_buddha

    private var selectedPosition = 0

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemBuddhaBinding>()
        val item = get(position) ?: return

        // 加载佛像图片（buddhaModuleUrl）
        binding.ivBuddha.loadImgNoHolder(item.buddhaModuleUrl)
        binding.tvBuddhaName.text = item.buddhaName ?: ""

        // 选中态样式
        val isSelected = position == selectedPosition
        binding.llParent.alpha = if (isSelected) 1.0f else 0.5f

        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != selectedPosition) {
                val oldPos = selectedPosition
                selectedPosition = pos
                notifyItemChanged(oldPos)
                notifyItemChanged(selectedPosition)
                onItemSelected(pos)
            }
        }
    }

    /**
     * 更新选中位置（供外部滑动联动调用）
     */
    fun setSelectedPosition(position: Int) {
        if (position == selectedPosition || position < 0 || position >= itemCount) return
        val oldPos = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPos)
        notifyItemChanged(selectedPosition)
    }
}

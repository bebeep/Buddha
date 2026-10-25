package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fingertip.baselib.bean.DonateBuddhaBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemDonateBuddhaBinding

/**
 * 功德佛像选择 Adapter（横滑单选）
 */
class DonateBuddhaAdapter(
    context: Context,
    private val onSelect: (DonateBuddhaBean) -> Unit
) : TopRcAdapter<DonateBuddhaBean, TopRcAdapter.TopRcViewHolder>(context) {

    var selectedId = -1
        private set

    override fun initLayoutId(viewType: Int): Int = R.layout.item_donate_buddha

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemDonateBuddhaBinding>()
        val item = get(position) ?: return

        binding.tvName.text = item.buddhaName
        if (!item.buddhaCoverUrl.isNullOrBlank()) {
            binding.ivCover.loadImg(item.buddhaCoverUrl)
        }
        binding.ivCover.setBackgroundResource(
            if (item.id == selectedId) R.drawable.bg_praying_cover_selected
            else R.drawable.bg_praying_cover_normal
        )
        holder.itemView.setOnClickListener {
            selectedId = item.id
            notifyDataSetChanged()
            onSelect(item)
        }
    }

    fun selectFirst() {
        if (selectedId == -1) {
            get(0)?.let {
                selectedId = it.id
                onSelect(it)
                notifyDataSetChanged()
            }
        }
    }
}

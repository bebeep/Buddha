package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.BuddhaBook
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemBuddhaBookBinding

/**
 * 佛像详情-推荐佛经 Adapter（横滑卡片），点击跳经书详情
 */
class BuddhaDetailBookAdapter(
    context: Context,
    list: List<BuddhaBook>,
    private val onBookClick: ((BuddhaBook) -> Unit)? = null
) : TopRcAdapter<BuddhaBook, TopRcAdapter.TopRcViewHolder>(context) {

    init {
        initData(list)
    }

    override fun initLayoutId(viewType: Int): Int = R.layout.item_buddha_book

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemBuddhaBookBinding>()
        val item = get(position) ?: return

        if (item.bookCover.isNotBlank()) {
            binding.ivBookCover.loadImg(item.bookCover)
        }
        binding.tvBookTitle.text = item.bookTitle

        holder.itemView.setOnClickListener {
            onBookClick?.invoke(item)
        }
    }
}

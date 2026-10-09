package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.BuddhaBook
import com.fingertip.uilib.fragment.book.FojingDetailsFragment
import com.fingertip.uilib.R
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.databinding.ItemFojingBinding
import com.weikaiyun.fragmentation.SupportActivity

/**
 * 佛经
 */
class FojingAdapter(context: Context):TopRcAdapter<BuddhaBook,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_fojing

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemFojingBinding>()
        get(position)?.let {
            binding.ivBook.loadImg(it.bookCover)
            binding.tvBookName.text = it.bookTitle
            binding.tvReadStatus.text = it.viewCount.toString()

            holder.itemView.setOnClickListener {
                (context as? SupportActivity)?.start(FojingDetailsFragment())
            }
        }
    }
}
package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.BuddhaBook
import com.fingertip.uilib.fragment.book.FojingDetailsFragment
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemBookShelfBinding
import com.weikaiyun.fragmentation.SupportActivity

/**
 * 书架
 */
class BookShelfAdapter(context: Context):TopRcAdapter<BuddhaBook,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_book_shelf

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemBookShelfBinding>()
        get(position)?.let {
            binding.ivBook.loadImg(it.bookCover)
            binding.tvBookName.text = it.bookTitle
            if (it.copyPercent > 0)
            {
                binding.tvReadStatus.text = "已抄写${it.copyPercent}%"
            }
            else
            {
                binding.tvReadStatus.text = "已读${it.readPercent}%"
            }

            holder.itemView.setOnClickListener {
                (context as? SupportActivity)?.start(FojingDetailsFragment())
            }
        }
    }
}
package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.BookSubjectEntity
import com.fingertip.uilib.R
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.databinding.ItemSubjectBinding

/**
 * 专题
 */
class SubjectAdapter(context: Context,val onItemClick:(pos:Int)->Unit):TopRcAdapter<BookSubjectEntity,TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int) = R.layout.item_subject

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemSubjectBinding>()
        get(position)?.let {
            binding.ivBg.loadImg(it.themeCover)
            binding.tvTitle.text = it.themeTitle ?: ""
            binding.tvIntro.text = it.themeIntro ?: ""
            binding.tvFollowed.text = it.onShelfCount.toString()
            binding.tvBookCount.text = it.bookCount.toString()
            holder.itemView.setOnClickListener {
                onItemClick(position)
            }
        }
    }
}
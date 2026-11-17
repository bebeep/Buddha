package com.fingertip.uilib.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.databinding.ItemTempleCoverBinding

/**
 * 古刹封面轮播 Adapter（ViewPager2 用）：
 * 只负责展示图片，点击行为由外部 onImageClick 处理（跳大图预览）
 */
class TempleCoverAdapter(
    private val images: List<String>,
    private val onImageClick: (index: Int) -> Unit
) : RecyclerView.Adapter<TempleCoverAdapter.Holder>() {

    class Holder(val binding: ItemTempleCoverBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemTempleCoverBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun getItemCount(): Int = images.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.ivPage.loadImg(images[position])
        holder.binding.ivPage.setOnClickListener { onImageClick(position) }
    }
}

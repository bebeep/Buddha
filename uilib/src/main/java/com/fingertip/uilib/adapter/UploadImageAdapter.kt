package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemUploadImageBinding


/**
 * 发布动态
 * 图片列表
 */
class UploadImageAdapter(var c: Context, var onItemClick: (pos:Int,tag:Int)->Unit) : TopRcAdapter<String?, TopRcAdapter.TopRcViewHolder>(c) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_upload_image

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemUploadImageBinding>()
        val hasImage = get(position) != null

        binding.ivAdd.visibility = if (!hasImage) View.VISIBLE else View.GONE
        binding.ivDelete.visibility = if (!hasImage) View.GONE else View.VISIBLE

        if (!hasImage) {
            binding.ivPhoto.setImageBitmap(null)
        }

        get(position)?.let {
            Glide.with(c).asBitmap().load(it).into(binding.ivPhoto)
            binding.ivDelete.setOnClickListener { onItemClick(position, 0) }
        }

        holder.itemView.setOnClickListener { onItemClick(position, 1) }
    }
}

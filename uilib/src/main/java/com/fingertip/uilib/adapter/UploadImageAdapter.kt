package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import kotlinx.android.synthetic.main.item_upload_image.view.*


/**
 * 发布动态
 * 图片列表
 */
class UploadImageAdapter(var c: Context, var onItemClick: (pos:Int,tag:Int)->Unit) : TopRcAdapter<String?, TopRcAdapter.TopRcViewHolder>(c) {


    override fun initLayoutId(viewType: Int): Int = R.layout.item_upload_image

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        holder.itemView.iv_add.visibility = if ( get(position) == null) View.VISIBLE else View.GONE
        holder.itemView.iv_delete.visibility = if ( get(position) == null) View.GONE else View.VISIBLE
        if ( get(position) == null)holder.itemView.iv_photo.setImageBitmap(null)
        get(position)?.let {
            Glide.with(c).asBitmap().load(it).into(holder.itemView.iv_photo)
            holder.itemView.iv_delete.setOnClickListener { onItemClick(position,0) }
        }

        holder.itemView.setOnClickListener { onItemClick(position,1) }

    }
}
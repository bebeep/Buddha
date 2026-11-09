package com.fingertip.uilib.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.fingertip.baselib.bean.MyDressData
import com.fingertip.baselib.bean.MyDressItem
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemDressCaseBinding

/**
 * 我的页装扮橱窗（§12.6）：横滑展示持有的装扮，
 * 当前佩戴的项金描边 + "使用中"角标；空列表由页面层显示占位文案。
 */
class DressCaseAdapter(
    context: Context,
    private val dressData: MyDressData,
    val onItemClick: (item: MyDressItem) -> Unit = {}
) : TopRcAdapter<MyDressItem, DressCaseAdapter.DressCaseVH>(context) {

    init {
        //我的页装扮橱窗横滑子列表，空列表由页面层显示占位文案
        showEmptyTip = false
    }

    override fun initLayoutId(viewType: Int): Int = R.layout.item_dress_case

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DressCaseVH {
        val binding = ItemDressCaseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DressCaseVH(binding)
    }

    override fun onBindViewHolder(holder: DressCaseVH, position: Int) {
        val binding = holder.binding
        val item = get(position) ?: return

        binding.tvDressName.text = item.dressName ?: ""
        val wearing = dressData.isWearing(item)
        binding.tvWearing.visibility = if (wearing) View.VISIBLE else View.GONE
        binding.ivDress.setBackgroundResource(
            if (wearing) R.drawable.bg_dress_case_wearing else R.drawable.bg_dress_case_normal
        )
        val url = item.previewImage
        if (url.isNullOrEmpty()) {
            binding.ivDress.setImageResource(com.fingertip.baselib.R.mipmap.icon_default_img)
        } else {
            Glide.with(binding.ivDress).load(url).into(binding.ivDress)
        }
        binding.root.setOnClickListener { onItemClick(item) }
    }

    class DressCaseVH(val binding: ItemDressCaseBinding) : TopRcViewHolder(binding.root)
}

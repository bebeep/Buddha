package com.fingertip.uilib.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.BuddhaVideo
import com.fingertip.baselib.util.loadImgNoHolder
import com.fingertip.uilib.databinding.ItemBuddhaVideoBinding

/**
 * 佛像详情「视频简介」横向卡片适配器：
 * 封面 + 播放钮 + 时长 + 「1/4 标题」页码条，对应原型「佛像详情」底部的视频区块
 */
class BuddhaVideoAdapter(
    private val onVideoClick: (position: Int) -> Unit
) : RecyclerView.Adapter<BuddhaVideoAdapter.VideoHolder>() {

    private val items = ArrayList<BuddhaVideo>()

    fun submit(list: List<BuddhaVideo>) {
        items.clear()
        items.addAll(list.filterNotNull())
        notifyDataSetChanged()
    }

    fun videos(): List<BuddhaVideo> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        val binding = ItemBuddhaVideoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VideoHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VideoHolder, position: Int) {
        holder.bind(items[position], position, items.size)
    }

    inner class VideoHolder(val binding: ItemBuddhaVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BuddhaVideo, position: Int, total: Int) {
            binding.ivCover.loadImgNoHolder(item.videoCover)
            binding.tvDuration.text = formatDuration(item.videoDuration)
            binding.tvTitle.text = "${position + 1}/$total  ${item.videoTitle.orEmpty()}"
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onVideoClick(pos)
            }
        }

        private fun formatDuration(sec: Int): String {
            if (sec <= 0) return ""
            val m = sec / 60
            val s = sec % 60
            return String.format("%02d:%02d", m, s)
        }
    }
}

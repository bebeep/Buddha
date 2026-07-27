package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemUploadImageBinding

/**
 * 上传状态枚举
 */
enum class UploadStatus {
    IDLE,       // 默认状态（未上传）
    UPLOADING,  // 上传中
    SUCCESS,    // 上传成功
    FAIL        // 上传失败
}

/**
 * 发布动态
 * 图片列表
 */
class UploadImageAdapter(var c: Context, var onItemClick: (pos: Int, isDelete: Boolean) -> Unit) :
    TopRcAdapter<String?, TopRcAdapter.TopRcViewHolder>(c) {

    // 每个位置的上传状态
    private val statusMap = HashMap<Int, UploadStatus>()

    override fun initLayoutId(viewType: Int): Int = R.layout.item_upload_image

    override fun initData(list: List<String?>?) {
        statusMap.clear()
        super.initData(list)
    }

    /**
     * 设置某个位置的上传状态并刷新
     */
    fun setUploadStatus(position: Int, status: UploadStatus) {
        statusMap[position] = status
        notifyItemChanged(position)
    }

    /**
     * 批量设置上传状态
     */
    fun setUploadStatuses(statuses: Map<Int, UploadStatus>) {
        statusMap.putAll(statuses)
        notifyDataSetChanged()
    }

    /**
     * 清除所有上传状态
     */
    fun clearAllStatus() {
        statusMap.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemUploadImageBinding>()
        val hasImage = get(position) != null
        val status = statusMap[position] ?: UploadStatus.IDLE

        // 添加按钮
        binding.ivAdd.visibility = if (!hasImage) View.VISIBLE else View.GONE
        // 删除按钮：有图片且不在上传中时显示
        binding.ivDelete.visibility = if (!hasImage || status == UploadStatus.UPLOADING) View.GONE else View.VISIBLE

        // 上传状态 UI
        when (status) {
            UploadStatus.UPLOADING -> {
                binding.vStatusOverlay.visibility = View.VISIBLE
                binding.pbLoading.visibility = View.VISIBLE
                binding.tvSuccess.visibility = View.GONE
                binding.tvFail.visibility = View.GONE
            }
            UploadStatus.SUCCESS -> {
                binding.vStatusOverlay.visibility = View.GONE
                binding.pbLoading.visibility = View.GONE
                binding.tvSuccess.visibility = View.VISIBLE
                binding.tvFail.visibility = View.GONE
            }
            UploadStatus.FAIL -> {
                binding.vStatusOverlay.visibility = View.VISIBLE
                binding.pbLoading.visibility = View.GONE
                binding.tvSuccess.visibility = View.GONE
                binding.tvFail.visibility = View.VISIBLE
            }
            UploadStatus.IDLE -> {
                binding.vStatusOverlay.visibility = View.GONE
                binding.pbLoading.visibility = View.GONE
                binding.tvSuccess.visibility = View.GONE
                binding.tvFail.visibility = View.GONE
            }
        }

        if (!hasImage) {
            binding.ivPhoto.setImageBitmap(null)
        }

        get(position)?.let {
            Glide.with(c).asBitmap().load(it).into(binding.ivPhoto)
            // 上传中不允许删除
            if (status != UploadStatus.UPLOADING) {
                binding.ivDelete.setOnClickListener { onItemClick(position, true) }
            }
        }

        // 上传中不允许预览大图
        if (status != UploadStatus.UPLOADING) {
            holder.itemView.setOnClickListener { onItemClick(position, false) }
        } else {
            holder.itemView.setOnClickListener(null)
        }
    }
}

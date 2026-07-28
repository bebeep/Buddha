package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.dialog.BigImageDialog
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemMomentBinding

class MomentAdapter(context: Context, val onItemClick: (entity: MomentEntity) -> Unit) : TopRcAdapter<MomentEntity, TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int): Int = R.layout.item_moment

    var bigImageDialog: BigImageDialog? = null

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemMomentBinding>()
        get(position)?.let {
            binding.vDivider.visibility = if (position == mlist.size - 1) View.GONE else View.VISIBLE
            binding.flImageVideo.visibility = if (it.momentType == 1) View.GONE else View.VISIBLE
            binding.cvVideo.visibility = if (it.momentType == 2) View.VISIBLE else View.GONE
            binding.rvPhotos.visibility = if (it.momentType == 3) View.VISIBLE else View.GONE
            binding.tvContent.visibility = if (it.textContent.isNullOrEmpty()) View.GONE else View.VISIBLE

            binding.tvContent.text = it.textContent
            binding.tvName.text = it.postUserName
            binding.tvLevel.text = "lv.${it.userLevel}"
            binding.ivHead.loadHead(it.postAvatar)
            if (it.momentType == 2) //显示视频
            {
                binding.video.loadCoverImage(it.videoCover, com.fingertip.baselib.R.mipmap.icon_default_img)
            }
            else if (it.momentType == 3) {//显示图片集合
                val columns = when (it.imageUrl.size) {
                    1 -> 1 //单图
                    4 -> 2 //两列
                    else -> 3 //三列
                }
                binding.rvPhotos.layoutManager = GridLayoutManager(context, columns)
                val adapter = MomentImageAdapter(context) { pos ->
                    bigImageDialog = BigImageDialog(context, it.imageUrl, pos)
                    bigImageDialog?.show()
                }

                binding.rvPhotos.adapter = adapter
                adapter.initData(it.imageUrl)

                holder.itemView.setOnClickListener { _ ->
                    onItemClick(it)
                }
            }
        }
    }
}

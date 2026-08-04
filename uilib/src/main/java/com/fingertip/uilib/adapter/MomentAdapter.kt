package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.dialog.BigImageDialog
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.TimeUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemMomentBinding

class MomentAdapter(
    context: Context,
    val onItemClick: (entity: MomentEntity) -> Unit
) : TopRcAdapter<MomentEntity, TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int): Int = R.layout.item_moment

    var bigImageDialog: BigImageDialog? = null

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemMomentBinding>()
        get(position)?.let { entity ->
            binding.vDivider.visibility = if (position == mlist.size - 1) View.GONE else View.VISIBLE
            binding.flImageVideo.visibility = if (entity.momentType == 1) View.GONE else View.VISIBLE
            binding.cvVideo.visibility = if (entity.momentType == 2) View.VISIBLE else View.GONE
            binding.rvPhotos.visibility = if (entity.momentType == 3) View.VISIBLE else View.GONE
            binding.tvContent.visibility = if (entity.textContent.isNullOrEmpty()) View.GONE else View.VISIBLE



            binding.tvContent.text = entity.textContent
            binding.tvName.text = entity.postUserName
            binding.tvLevel.text = "lv.${entity.userLevel}"
            binding.ivHead.loadHead(entity.postAvatar)
            binding.tvLike.text = "${entity.likeCount}"
            binding.tvComment.text = "${entity.commentCount}"
            binding.tvFollow.isSelected = entity.isFollowed
            binding.tvTimeLocate.text = "${TimeUtil.dateFormateTime(entity.createDate)}·${entity.postAddress}"
            if (entity.momentType == 2) //视频
            {
                //根据视频宽高比设置容器高度
                if (entity.videoWidth > 0 && entity.videoHeight > 0) {
                    val screenWidth = binding.cvVideo.resources.displayMetrics.widthPixels - (binding.cvVideo.resources.displayMetrics.density * 16 * 2).toInt()
                    if (entity.videoHeight > entity.videoWidth)
                    {
                        binding.cvVideo.layoutParams = (binding.cvVideo.layoutParams as ViewGroup.LayoutParams).apply {
                            width = (screenWidth * 0.6).toInt()
                            height = (screenWidth * 0.6 * (entity.videoHeight.toFloat()/entity.videoWidth.toFloat())).toInt()
                        }
                    }
                    else
                    {
                        binding.cvVideo.layoutParams = (binding.cvVideo.layoutParams as ViewGroup.LayoutParams).apply {
                            width = screenWidth
                            height = (screenWidth * entity.videoHeight.toFloat() / entity.videoWidth.toFloat()).toInt()
                        }
                    }
                }
                //加载封面
                binding.video.loadCoverImage(entity.videoCover, com.fingertip.baselib.R.mipmap.icon_default_img)
            }
            else if (entity.momentType == 3) {//显示图片集合
                val columns = when (entity.imageUrl.size) {
                    1 -> 1 //单图
                    4 -> 2 //两列
                    else -> 3 //三列
                }
                binding.rvPhotos.layoutManager = GridLayoutManager(context, columns)
                val adapter = MomentImageAdapter(context) { pos ->
                    bigImageDialog = BigImageDialog(context, entity.imageUrl, pos)
                    bigImageDialog?.show()
                }

                binding.rvPhotos.adapter = adapter
                adapter.initData(entity.imageUrl)
            }

            holder.itemView.setOnClickListener { _ ->
                onItemClick(entity)
            }
        }
    }
}

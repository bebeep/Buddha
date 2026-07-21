package com.fingertip.uilib.adapter

import android.content.Context
import androidx.constraintlayout.widget.ConstraintSet
import com.blankj.utilcode.util.SizeUtils
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemMomentImageBinding


/**
 * 图片列表
 */
class MomentImageAdapter(
    val c: Context,
    val singleImageWidth: Int = 0,
    val singleImageHeight: Int = 0,
    val onItemClick: (position: Int) -> Unit
) : TopRcAdapter<String?, TopRcAdapter.TopRcViewHolder>(c) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_moment_image

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemMomentImageBinding>()
        val isHorizontal = singleImageWidth > singleImageHeight
        get(position)?.let {
            if (mlist.size == 1) {
                val param = binding.clMomentImage.layoutParams
                param.width = if (isHorizontal) SizeUtils.dp2px(273f) else SizeUtils.dp2px(162f)
                binding.clMomentImage.layoutParams = param
            }

            val width = if (mlist.size == 1) singleImageWidth / 2 else 400
            val height = if (mlist.size == 1) singleImageHeight / 2 else 400
            binding.ivContent.loadImg(it, width = width, height = height)
            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.clMomentImage)
            //横图：固定宽度270dp
            //竖图：固定宽度159dp,最大高度255dp
            if (mlist.size == 1) {
                if (singleImageWidth <= 0 || singleImageHeight <= 0) constraintSet.setDimensionRatio(R.id.iv_content, "v,138:208")
                else {
                    val scale = if (!isHorizontal && (singleImageWidth * 1.0) / (singleImageHeight * 1.0) < 159.0 / 255.0) "159:255" else "$singleImageWidth:$singleImageHeight"
                    constraintSet.setDimensionRatio(R.id.iv_content, "${if (isHorizontal) "h" else "v"},$scale")
                }
            } else constraintSet.setDimensionRatio(R.id.iv_content, "h,1:1")
            constraintSet.applyTo(binding.clMomentImage)
            binding.ivContent.setOnClickListener {
                onItemClick(position)
            }
        }
    }
}

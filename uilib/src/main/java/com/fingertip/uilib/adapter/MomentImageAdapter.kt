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
 *
 * 这个 adapter 是跟着父 recycler 的 holder 一起复用的，
 * 所以宽高比例、单图宽度这些只在实际变化时才改，别每次 bind 都 clone 一遍 ConstraintSet。
 */
class MomentImageAdapter(
    val c: Context,
    val singleImageWidth: Int = 0,
    val singleImageHeight: Int = 0,
    val onItemClick: (position: Int) -> Unit
) : TopRcAdapter<String?, TopRcAdapter.TopRcViewHolder>(c) {

    /** 当前已经应用过的比例，避免重复 apply */
    private var appliedRatio: String? = null

    /** 当前已经应用过的单图宽度 */
    private var appliedWidth = -1

    override fun initLayoutId(viewType: Int): Int = R.layout.item_moment_image

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemMomentImageBinding>()
        val isHorizontal = singleImageWidth > singleImageHeight
        get(position)?.let {
            if (mlist.size == 1) {
                val targetWidth = if (isHorizontal) SizeUtils.dp2px(273f) else SizeUtils.dp2px(162f)
                if (appliedWidth != targetWidth) {
                    appliedWidth = targetWidth
                    val param = binding.clMomentImage.layoutParams
                    param.width = targetWidth
                    binding.clMomentImage.layoutParams = param
                }
            }

            val width = if (mlist.size == 1) singleImageWidth / 2 else 400
            val height = if (mlist.size == 1) singleImageHeight / 2 else 400
            binding.ivContent.loadImg(it, width = width, height = height)

            //横图：固定宽度270dp
            //竖图：固定宽度159dp,最大高度255dp
            val ratio = if (mlist.size == 1) {
                when {
                    singleImageWidth <= 0 || singleImageHeight <= 0 -> "v,138:208"
                    else -> {
                        val scale =
                            if (!isHorizontal && (singleImageWidth * 1.0) / (singleImageHeight * 1.0) < 159.0 / 255.0) "159:255"
                            else "$singleImageWidth:$singleImageHeight"
                        "${if (isHorizontal) "h" else "v"},$scale"
                    }
                }
            } else "h,1:1"
            applyRatio(binding, ratio)

            binding.ivContent.setOnClickListener {
                onItemClick(position)
            }
        }
    }

    private fun applyRatio(binding: ItemMomentImageBinding, ratio: String) {
        if (ratio == appliedRatio) return
        appliedRatio = ratio
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.clMomentImage)
        constraintSet.setDimensionRatio(R.id.iv_content, ratio)
        constraintSet.applyTo(binding.clMomentImage)
    }
}

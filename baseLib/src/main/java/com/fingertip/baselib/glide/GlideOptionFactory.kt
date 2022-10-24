package com.fingertip.baselib.glide

import androidx.annotation.DrawableRes
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.Downsampler
import com.bumptech.glide.request.RequestOptions
import com.lzlz.toplib.extention.toPx


object GlideOptionFactory {

    fun buildUniversalOption(
        width: Int?,
        height: Int?,
        @DrawableRes holder: Int?,
        transform: BitmapTransformation?,
        cropImage: Boolean,
        useArgb8888: Boolean = false,
        url: Any? = null
    ): RequestOptions {
        return RequestOptions()
            .apply {
                if (cropImage) {
                    centerCrop()
                }
            }
            .apply {
                if (width != null && height != null) {
                    override(width.toPx(), height.toPx())
                }
            }
            .apply {
                transform?.let {
                    transform(it)
                }
            }
            .apply {
                holder?.let {
                    placeholder(it).error(it)
                }
            }
            .apply {
                if (useArgb8888 || (url is String && url.contains(".gif"))) {
                    format(DecodeFormat.PREFER_ARGB_8888)
                } else {
                    format(DecodeFormat.PREFER_RGB_565).set(Downsampler.ALLOW_HARDWARE_CONFIG, true)
                }
//                set(GifOptions.DECODE_FORMAT, DecodeFormat.PREFER_RGB_565)
            }
    }
}
package com.fingertip.baselib.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.fingertip.baselib.R
import com.fingertip.baselib.glide.GlideOptionFactory
import com.fingertip.baselib.glide.NoTransform


fun ImageView.loadImg(
    url: Any?,
    width: Int? = null,
    height: Int? = null,
    @DrawableRes holder: Int? = R.mipmap.icon_default_img,
    transform: BitmapTransformation? = null,
    cropImage: Boolean = true,
    useArgb8888: Boolean = false,
    option: RequestOptions = GlideOptionFactory.buildUniversalOption(
                width,
                height,
                holder,
                transform,
                cropImage,
                useArgb8888,
                url
        ),
    listener: RequestListener<Drawable>? = null
) {
    if (url is String && (url.endsWith(".gif") || url.endsWith(".GIF"))){
        Glide.with(this)
            .asGif()
            .load(url)
            .apply(option)
            .into(this)
        return
    }
    Glide.with(this)
            .load(url)
            .apply(option)
            .apply {
                if (listener != null) {
                    listener(listener)
                }
            }
            .into(this)

}

/**
 * 加载用户头像。约定：所有用户头像一律圆形展示，故默认 transform = CircleCrop()，
 * centerCrop() 与圆形裁剪叠加后即为正圆（含占位图 icon_default_head 也裁圆）。
 * 调用方无需再单独处理圆形。
 */
fun ImageView.loadHead(
        url: Any?,
        width: Int? = null,
        height: Int? = null,
        @DrawableRes holder: Int? = R.mipmap.icon_default_head,
        transform: BitmapTransformation? = CircleCrop(),
        listener: RequestListener<Drawable>? = null
) {
    loadImg(url, width, height, holder, transform, listener = listener)
}

fun ImageView.loadWebpGif(drawableId: Any) {

    val circleCrop: Transformation<Bitmap> =
        NoTransform()
    val webpDrawableTransformation =
            WebpDrawableTransformation(circleCrop)

    Glide.with(this)
            .load(drawableId)
            .optionalTransform(WebpDrawable::class.java, webpDrawableTransformation)
            .into(this)
}

fun ImageView.loadImgNoHolder(   url: Any?){
    Glide.with(this)
        .apply {
            if (url is String && url.contains(".gif")) {
                this.asGif().diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            }
        }
        .load(url)

        .into(this)

}



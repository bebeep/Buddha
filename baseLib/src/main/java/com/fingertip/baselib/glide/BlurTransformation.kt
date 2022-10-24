package com.fingertip.baselib.glide

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.fingertip.baselib.top.TopApplication
import java.security.MessageDigest

/**
 * 模糊
 */
class BlurTransformation : BitmapTransformation {
    private var blurRadius = 5f

    constructor() : super() {}
    constructor(blurRadius: Float) : super() {
        this.blurRadius = blurRadius
    }

    protected override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        return blurBitmap(toTransform, blurRadius)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("blur transformation".toByteArray())
    }

    companion object {
        // 图片缩放比例(即模糊度)
        private const val BITMAP_SCALE = 0.3f

        /**
         * @param image      需要模糊的图片
         * @param blurRadius 0 - 25
         * @return 模糊处理后的Bitmap
         */
        fun blurBitmap(image: Bitmap, blurRadius: Float): Bitmap {
            var image: Bitmap = image
            if (image.getConfig() != Bitmap.Config.ARGB_8888) {
                image = image.copy(Bitmap.Config.ARGB_8888, true)
            }

            // 计算图片缩小后的长宽
            val width = Math.round(image.getWidth() * BITMAP_SCALE)
                .toInt()
            val height = Math.round(image.getHeight() * BITMAP_SCALE)
                .toInt()

            // 将缩小后的图片做为预渲染的图片
            //Bitmap blurredBitmap = toTransform.copy(Bitmap.Config.ARGB_8888, true);
            val inputBitmap: Bitmap = Bitmap.createScaledBitmap(image, width, height, false)
            // 创建一张渲染后的输出图片
            val outputBitmap: Bitmap = Bitmap.createBitmap(inputBitmap)

            // 创建RenderScript内核对象
            val rs: RenderScript = RenderScript.create(TopApplication.instance)
            // 创建一个模糊效果的RenderScript的工具对象
            val blurScript: ScriptIntrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

            // 由于RenderScript并没有使用VM来分配内存,所以需要使用Allocation类来创建和分配内存空间
            // 创建Allocation对象的时候其实内存是空的,需要使用copyTo()将数据填充进去
            //Allocation input = Allocation.createFromBitmap(rs, blurredBitmap, Allocation.MipmapControl.MIPMAP_FULL, Allocation.USAGE_SHARED);
            val tmpIn: Allocation = Allocation.createFromBitmap(rs, inputBitmap)
            val tmpOut: Allocation = Allocation.createFromBitmap(rs, outputBitmap)

            // 设置渲染的模糊程度, 25f是最大模糊度
            blurScript.setRadius(blurRadius)
            // 设置blurScript对象的输入内存
            blurScript.setInput(tmpIn)
            // 将输出数据保存到输出内存中
            blurScript.forEach(tmpOut)

            // 将数据填充到Allocation中
            tmpOut.copyTo(outputBitmap)
            return outputBitmap
        }
    }
}
package com.fingertip.baselib.glide

import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import android.graphics.Bitmap
import java.security.MessageDigest

class NoTransform : BitmapTransformation() {
    // Bitmap doesn't implement equals, so == and .equals are equivalent here.
    override fun transform(
        pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int
    ): Bitmap {
        return toTransform
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    companion object {
        // The version of this transformation, incremented to correct an error in a previous version.
        // See #455.
        private const val VERSION = 1
        private const val ID = "NoTransform." + VERSION
        private val ID_BYTES = ID.toByteArray(CHARSET)
    }
}
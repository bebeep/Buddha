package com.fingertip.baselib.glide

import android.content.Context
import android.util.Log
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.Downsampler

@GlideModule
class MyGlideApp : AppGlideModule() {
    /** * MemorySizeCalculator类通过考虑设备给定的可用内存和屏幕大小想出合理的默认大小. * 通过LruResourceCache进行缓存。 * @param context * @param builder  */
    override fun applyOptions(context: Context, builder: GlideBuilder) {

        //设置内存缓存
        val maxMemory = Runtime.getRuntime().maxMemory().toInt()
        val memoryCacheSize = maxMemory / 8
        builder.setMemoryCache(LruResourceCache(memoryCacheSize.toLong()))
        val cacheDir = context.cacheDir
        if (cacheDir != null) {
            val diskCacheSize = 1024 * 1024 * 512
            builder.setDiskCache(
                DiskLruCacheFactory(
                    cacheDir.absolutePath,
                    "Images",
                    diskCacheSize.toLong()
                )
            )
        }

        //设置Bitmap的缓存池
        val bitmapPoolSizeBytes = 1024 * 1024 * 30
        builder.setBitmapPool(LruBitmapPool(bitmapPoolSizeBytes.toLong()))

//        //设置磁盘缓存
//        builder.setDiskCache(new InternalCacheDiskCacheFactory(context));

        //设置读取不在缓存中资源的线程
//        builder.setResizeExecutor(GlideExecutor.newSourceExecutor());
//
//        //设置读取磁盘缓存中资源的线程
//        builder.setDiskCacheExecutor(GlideExecutor.newDiskCacheExecutor());
//
//        //设置日志级别
        builder.setLogLevel(Log.ERROR)

        //设置全局选项
        builder.setDefaultRequestOptions(
            RequestOptions().format(DecodeFormat.PREFER_RGB_565)
                .set(Downsampler.ALLOW_HARDWARE_CONFIG, true)
        )
    }
}
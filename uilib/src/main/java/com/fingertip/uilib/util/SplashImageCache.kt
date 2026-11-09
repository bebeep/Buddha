package com.fingertip.uilib.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.blankj.utilcode.util.SPUtils
import java.io.File
import java.io.FileOutputStream

/**
 * 启动页图片本地缓存。
 *
 * 设计要点：
 * - 缓存目录用 filesDir（持久，系统不会像 cacheDir 那样清理，否则下次启动会空白）。
 * - 首帧仍由编译期 windowBackground(bg_startup) 兜底，本类只负责"第二帧起"用本地图盖满，
 *   做到视觉连续、不空白、不联网等待。
 * - 首次启动本地无图，只能先显示兜底图 + 后台拉取存盘，第二次启动才显示服务端图（物理限制）。
 * - 存储的是按屏幕分辨率缩放压缩后的小 JPEG，显示时解码快、省内存。
 */
object SplashImageCache {
    private const val DIR = "splash"
    private const val FILE = "splash.jpg"
    private const val K_URL = "splash_url"
    private const val K_EXP = "splash_expire"

    /** 返回有效缓存文件，不存在/过小视为无效（防损坏图黑屏） */
    fun getCacheFile(ctx: Context): File? {
        val f = File(ctx.filesDir, "$DIR/$FILE")
        return if (f.exists() && f.length() > 1024) f else null
    }

    /** 缓存是否过期（到点重新拉取） */
    fun isExpired(): Boolean {
        val exp = SPUtils.getInstance().getLong(K_EXP, 0)
        return exp != 0L && System.currentTimeMillis() > exp
    }

    /** 保存解码后的位图（已按需缩放），压缩为 JPEG 落盘 */
    fun save(ctx: Context, bmp: Bitmap, expireDays: Int = 7) {
        val dir = File(ctx.filesDir, DIR)
        dir.mkdirs()
        FileOutputStream(File(dir, FILE)).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
        }
        SPUtils.getInstance().put(K_EXP, System.currentTimeMillis() + expireDays * 86400000L)
    }

    fun currentUrl() = SPUtils.getInstance().getString(K_URL, "")
    fun setCurrentUrl(url: String) = SPUtils.getInstance().put(K_URL, url)

    /**
     * 子线程安全的解码：先 inJustDecodeBounds 量尺寸，按目标宽高算 inSampleSize 缩放，
     * 再 RGB_565 解码，避免大图在主线程/内存上卡顿。
     */
    fun decodeSampled(path: String, reqW: Int, reqH: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts) ?: return null
        opts.inSampleSize = calculateInSampleSize(opts, reqW, reqH)
        opts.inJustDecodeBounds = false
        opts.inPreferredConfig = Bitmap.Config.RGB_565
        return try {
            BitmapFactory.decodeFile(path, opts)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(o: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        val (h, w) = o.outHeight to o.outWidth
        var s = 1
        while (h / s > reqH || w / s > reqW) s *= 2
        return s
    }
}

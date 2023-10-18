package com.fingertip.uilib.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import android.util.Log
import android.widget.TextView
import com.fingertip.baselib.bean.PhotoEntity
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.fragment.moment.HandleVideoFragment
import com.weikaiyun.fragmentation.SupportActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.collections.ArrayList


/**
 * 朋友圈相关工具类
 */
object MomentUtils {




    //点击textView中的link
    fun autoLinkCLick(textView:TextView?){
        textView?.let {
            val text = it.text
            if (text is Spannable) {
                val urls = text.getSpans(0, text.length, URLSpan::class.java)
                var newText = text.toString()
                for (urlSpan in urls) newText = newText.replace(urlSpan.url, "[Link]")
                val builder = SpannableStringBuilder(newText)
                builder.clearSpans()

                var linkIndex = -6
                for (urlSpan in urls) {
                    if (!text.contains(urlSpan.url)) continue
                    try {
                        linkIndex = newText.indexOf("[Link]", linkIndex + 6)
                        val myUrlSpan = MyUrlSpan(urlSpan.url)
                        builder.setSpan(myUrlSpan, linkIndex, linkIndex + 6, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
                    } catch (e: java.lang.Exception) {
                    }
                }
                textView.text = builder
            }
        }
    }




    var checkPornScore = 0.3
    /**
     * 批量上传
     */
    var photoList = ArrayList<PhotoEntity>()
    val compressSize = 1 * 1024 * 1024 //压缩的阈值 1M




    //获取视频宽高
    fun setVideoWidthAndHeight(photo: PhotoEntity) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(photo.path)
            val orientation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)//视频方向          0, 90, 180, or 270
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?:"0"
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?:"0"
            photo.width = width.toInt()
            photo.height = height.toInt()
            photo.size = FileInputStream(File(photo.path)).available().toLong() //视频大小
        } catch (e: java.lang.Exception) {
        }
    }

    /**
     * 获取图片宽高
     */
    fun setImageWidthAndHeight(photo: PhotoEntity) {
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(photo.path, options)
            photo.width = options.outWidth
            photo.height = options.outHeight
            photo.size = FileInputStream(File(photo.path)).available().toLong() //图片大小
        } catch (e: java.lang.Exception) {
        }
    }




    /**
     * 前往处理视频
     */
    const val REQUEST_CODE_HANDLE_VIDEO = 9912
    fun goHandleVideo(activity: SupportActivity, filePath:String){
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            val inputStream = FileInputStream(File(filePath).absolutePath)
            retriever.setDataSource(inputStream.fd)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?:"0"
            retriever.release()
            if (time.toLong() < 3000) {
                ToastUtil.showMessage("视频不能低于3秒")
                return
            }
            activity.startForResult(HandleVideoFragment().apply {
                path = filePath
            },REQUEST_CODE_HANDLE_VIDEO)
        } catch (e: Exception) {
            ToastUtil.showMessage("视频格式有误")
        }
    }


    // 获取视频缩略图
    fun getVideoThumb(filePath: String?): File? {
        var file: File? = null
        return try {
            val mmr = MediaMetadataRetriever() //实例化MediaMetadataRetriever对象
            val inputStream = FileInputStream(File(filePath).absolutePath)
            mmr.setDataSource(inputStream.fd) //设置数据源为该文件对象指定的绝对路径
            val bitmap = mmr.frameAtTime //获得视频第一帧的Bitmap对象
            if (bitmap != null) {
                Log.i("qiuqiu", "获取视频缩略图成功")
            } else {
                Log.i("qiuqiu", "获取视频缩略图失败")
            }
            val appDir = File(Environment.getExternalStorageDirectory(), "naughty/btimap")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }
            val fileName = System.currentTimeMillis().toString() + ".png"
            file = File(appDir, fileName)
            val fos = FileOutputStream(file)
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            file
        }
    }


}
package com.fingertip.baselib.util

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.fingertip.baselib.BuildConfig
import com.fingertip.baselib.bean.MediaInfo
import com.fingertip.baselib.constant.CodeConstant
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopApplication
import com.zxy.tiny.Tiny
import com.zxy.tiny.callback.FileCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.*
import androidx.core.graphics.scale


/**
 * 图片压缩、鉴黄、上传工具类
 */
class PicUtils {

    var TAG=this::class.java.simpleName
    val mCropressPath:String = TopApplication.instance.cacheDir.path + "/compress/"
    val mphotoPath:String = TopApplication.instance.cacheDir.path+"/takephoto.jpg"
    val mcopy:String = TopApplication.instance.cacheDir.path+"/copy/"
    var showLoad:()-> Any = {}


    //创建图片文件
    fun createImageFile(): File? {
        val image = isHaveOrCreateFile(mphotoPath)
        log(TAG,"createImageFile :" + image?.path)
        return image
    }

    //压缩
    fun density(path: String?,callback: FileCallback) {
        showLoad.invoke()
        val options = Tiny.FileCompressOptions()
        isHaveOrCreateFolder(mCropressPath)
        options.outfile = mCropressPath + "my_pic"+System.currentTimeMillis()+".jpg"
        options.quality = 50

        options.isKeepSampling = false
        Tiny.getInstance().source(path).asFile().withOptions(options).compress(callback)
    }

    fun getPhonePicPath(requestCode: Int, data: Intent?): String? {
        var originalImagePath: String? = null
        try {
            if (requestCode == CodeConstant.IntentSkipCode.REQUESTCODE_ALBUM && null != data) {
                val picturePath: String = SystemPhotoPathUtil.getPath(TopApplication.instance, data.data as Uri)?:""
                originalImagePath = picturePath
            } else if (requestCode == CodeConstant.IntentSkipCode.REQUESTCODE_CAMERA) {
                originalImagePath = getImagePath()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        log(TAG,"onActivityResult path : $originalImagePath")
        return originalImagePath
    }


    fun copyUriToExternalFilesDir(uri: Uri?, fileName: String) {
        isHaveOrCreateFolder(mcopy)
        if (uri==null){
            return
        }

        val inputStream = TopApplication.instance.contentResolver.openInputStream(uri)
        val tempDir = mcopy
        if (inputStream != null && tempDir != null) {
            val file = File("$tempDir/$fileName")
            if (file.exists()){
                file.delete()
            }

            val fos = FileOutputStream(file)
            val bis = BufferedInputStream(inputStream)
            val bos = BufferedOutputStream(fos)
            val byteArray = ByteArray(1024)
            var bytes = bis.read(byteArray)
            while (bytes > 0) {
                bos.write(byteArray, 0, bytes)
                bos.flush()
                bytes = bis.read(byteArray)
            }
            bos.close()
            fos.close()
        }
    }

    //返回图片地址
    fun getImagePath(): String? {
        return mphotoPath
    }

    fun uploadFile(path: String,needCheckPorn:Boolean = true) {
        upPicCallBack?.start()
        //离线鉴黄  失败就直接返回
//        if (needCheckPorn) NSFWHelper.getNSFWScore(path).let {
//            if (it.nsfwScore > 0.3){
//                upPicCallBack?.errorend()
//                if (BuildConfig.DEBUG)  ToastUtil.showMessage("鉴黄不通过-》分数：${it.nsfwScore}")
//                return
//            }
//            if (BuildConfig.DEBUG)  ToastUtil.showMessage("鉴黄通过-》分数：${it.nsfwScore}")
//        }
        MainScope().launch (Dispatchers.Default){
            //todo 上传服务器
        }

    }

    //Identify yellow pictures(鉴别是否黄图)
    private fun checkPicIsSelf(avatarPath: String) {
        log(TAG, "开始鉴黄")

    }

    private fun isHaveOrCreateFolder(filePath:String):File{
        val file = File(filePath)
        if (!file.exists()) {
            if (!file.mkdirs())
                log(TAG,"创建压缩文件夹失败 path:" + file.absolutePath)
        }
        return file
    }

    //没有文件就创建文件
    private fun isHaveOrCreateFile(filePath:String):File{
        val file = File(filePath)
        if (!file.exists()) {
            if (!file.createNewFile())
                log(TAG,"创建压缩文件夹失败 path:" + file.absolutePath)
        }
        return file
    }

    var upPicCallBack : UpPicCallBack?=null
    interface UpPicCallBack{
        fun start()
        fun isOktoUp(avatarPath: String)
        fun errorend()
    }



    /**
     * 媒体类型枚举
     */
    enum class MediaType {
        IMAGE,   // 图片
        VIDEO,   // 视频
        UNKNOWN  // 无效或无法识别的文件
    }

    /**
     * 通过解析文件头部数据判断媒体类型
     * @param filePath 本地文件的绝对路径
     * @return MediaType
     */
    fun getMediaType(filePath: String): MediaType {
        // 0. 文件存在性校验
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            return MediaType.UNKNOWN
        }

        // 1. 先尝试解析为图片（利用 BitmapFactory 仅解析边界，内存开销几乎为 0）
        val imageCheckResult = checkIsImage(filePath)
        if (imageCheckResult) {
            return MediaType.IMAGE
        }

        // 2. 再尝试解析为视频（利用 MediaMetadataRetriever 提取视频宽高）
        val videoCheckResult = checkIsVideo(filePath)
        if (videoCheckResult) {
            return MediaType.VIDEO
        }

        // 3. 都不是
        return MediaType.UNKNOWN
    }

    /**
     * 检查是否为有效图片
     * 原理：仅解析图片宽高，不将像素加载到内存
     */
    private fun checkIsImage(filePath: String): Boolean {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // 关键：仅解析边界信息
        }
        BitmapFactory.decodeFile(filePath, options)
        // 如果宽高都大于 0，说明是有效的图片格式（支持 JPEG, PNG, WebP, GIF 等）
        return options.outWidth > 0 && options.outHeight > 0
    }

    /**
     * 检查是否为有效视频
     * 原理：尝试读取视频元数据中的宽高
     */
    private fun checkIsVideo(filePath: String): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            width > 0 && height > 0
        } catch (_: Exception) {
            // 文件损坏、格式不支持或非视频文件都会走到这里
            false
        } finally {
            retriever.release()
        }
    }


    //根据文件路径获取媒体信息
    fun getMediaInfo(filePath: String, frameWidth: Int = 720): MediaInfo? {
        when (getMediaType(filePath)) {
            MediaType.IMAGE -> {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true // 只解析边界，不分配内存
                }
                BitmapFactory.decodeFile(filePath, options)
                return MediaInfo(
                    mediaType = 1,
                    mediaUrl = filePath,
                    thumbUrl = "",
                    width = options.outWidth,
                    height = options.outHeight,
                    duration = 0,
                    size = File(filePath).length()
                )
            }
            MediaType.VIDEO -> {
                val retriever = MediaMetadataRetriever()
                return try {
                    retriever.setDataSource(filePath)

                    // 1. 获取时长（单位：毫秒）
                    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

                    // 2. 获取首帧图（OPTION_NEXT_SYNC 表示寻找最近的关键帧，速度最快）
                    val originalFrame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_NEXT_SYNC)

                    // 3. 对首帧图进行缩放，防止 OOM（如果不需要缩放，直接传 originalFrame 即可）
                    val scaledFrame = if (frameWidth > 0 && originalFrame != null) {
                        val ratio = frameWidth.toFloat() / originalFrame.width
                        val height = (originalFrame.height * ratio).toInt()
                        originalFrame.scale(frameWidth, height)
                    } else {
                        originalFrame
                    }

                    val thumbUrl = saveThumbnailToCache(scaledFrame, filePath)

                    MediaInfo(
                        mediaType = 2,
                        mediaUrl = filePath,
                        thumbUrl = thumbUrl?: "",
                        width = scaledFrame?.width ?: 0,
                        height = scaledFrame?.height ?: 0,
                        duration = durationMs.toInt()/1000,
                        size = File(filePath).length()
                    )
                } catch (e: SecurityException) {
                    // 极少情况下会因文件权限抛出异常
                    e.printStackTrace()
                    null
                } catch (e: IllegalArgumentException) {
                    // 文件格式不支持或已损坏
                    e.printStackTrace()
                    null
                } finally {
                    retriever.release() // 必须释放，否则会持有文件句柄导致内存泄漏
                }
            }
            MediaType.UNKNOWN -> {
                return null
            }
        }
    }


    fun saveThumbnailToCache(bitmap: Bitmap?, videoFileName: String? = null): String? {
        if (bitmap == null) return null
        val fileName = if (videoFileName != null) {
            "thumb_${videoFileName.substringBeforeLast('.')}.jpg"
        } else {
            "thumb_${System.currentTimeMillis()}.jpg"
        }
        val file = File(TopApplication.instance.cacheDir, "thumbnails") // 子目录便于管理
        if (!file.exists()) file.mkdirs()

        val outputFile = File(file, fileName)
        return try {
            FileOutputStream(outputFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
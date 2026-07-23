package com.fingertip.baselib.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.OSS
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback
import com.alibaba.sdk.android.oss.common.OSSLog
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider
import com.alibaba.sdk.android.oss.model.GetObjectRequest
import com.alibaba.sdk.android.oss.model.GetObjectResult
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import com.fingertip.baselib.BuildConfig
import com.fingertip.baselib.bean.MediaInfo
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopApplication
import com.zxy.tiny.Tiny
import com.zxy.tiny.callback.FileCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.*
import java.util.concurrent.atomic.AtomicInteger
import androidx.core.graphics.scale
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider
import kotlin.random.Random


/**
 * 图片压缩、鉴黄、上传工具类
 */
object  PicUtils {


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
                    objectKey = "app/android/${System.currentTimeMillis()}_${Random.nextInt(1000)}_${File(filePath).name}",
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
                        objectKey = "app/android/${System.currentTimeMillis()}_${Random.nextInt(1000)}_${File(filePath).name}",
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


    // ==================== 阿里云 OSS 上传 / 下载 ====================

    private var ossClient: OSS? = null
    private var ossEndpoint: String = ""


    /**
     * 初始化 OSSClient（使用 AK/SK 方式）
     * @param endpoint OSS 服务 Endpoint，如 "https://oss-cn-hangzhou.aliyuncs.com"
     * @param accessKeyId AccessKey ID
     * @param accessKeySecret AccessKey Secret
     */
    fun initOss(endpoint: String, accessKeyId: String, accessKeySecret: String) {
        val credentialProvider = OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret)
        val conf = ClientConfiguration().apply {
            connectionTimeout = 15 * 1000
            socketTimeout = 15 * 1000
            maxConcurrentRequest = 5
            maxErrorRetry = 2
        }
        if (BuildConfig.DEBUG) {
            OSSLog.enableLog()
        }
        ossEndpoint = endpoint
        ossClient = OSSClient(TopApplication.instance.applicationContext, endpoint, credentialProvider, conf)
    }

    /**
     * 初始化 OSSClient（使用 STS Token 方式，推荐移动端使用）
     * @param endpoint OSS 服务 Endpoint，如 "https://oss-cn-hangzhou.aliyuncs.com"
     * @param stsAccessKeyId STS 临时 AccessKey ID
     * @param stsAccessKeySecret STS 临时 AccessKey Secret
     * @param stsSecurityToken STS Security Token
     */
    fun initOssWithSts(endpoint: String, credentialProvider: OSSCredentialProvider) {
        if (BuildConfig.DEBUG) {
            OSSLog.enableLog()
        }
        ossEndpoint = if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            endpoint
        } else {
            "https://$endpoint"
        }
        ossClient = OSSClient(TopApplication.instance.applicationContext, ossEndpoint, credentialProvider)
    }

    /**
     * 上传文件到阿里云 OSS
     * @param bucketName Bucket 名称
     * @param objectKey 文件在 OSS 中的 Key（含路径），如 "images/photo_123.jpg"
     * @param localFilePath 本地文件绝对路径
     * @param callback 上传回调（进度 / 成功 / 失败）
     */
    fun uploadToOss(bucketName: String,objectKey: String, localFilePath: String,callback: OssUploadCallback) {
        val client = ossClient
        if (client == null) {
            callback.onFailure("OSSClient 未初始化，请先调用 initOss() 或 initOssWithSts()")
            return
        }
        val file = File(localFilePath)
        if (!file.exists()) {
            callback.onFailure("文件不存在: $localFilePath")
            return
        }

        val putRequest = PutObjectRequest(bucketName, objectKey, localFilePath)
        // 设置上传进度回调
        putRequest.progressCallback = OSSProgressCallback { _, currentSize, totalSize -> callback.onProgress(currentSize, totalSize) }

        client.asyncPutObject(putRequest, object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
            override fun onSuccess(request: PutObjectRequest, result: PutObjectResult) {
                // 拼接文件访问 URL
                val url = "${ossEndpoint}/${bucketName}/${objectKey}"
                callback.onSuccess(url)
            }

            override fun onFailure(request: PutObjectRequest, clientException: ClientException?, serviceException: ServiceException?) {
                val msg = when {
                    clientException != null -> "客户端异常: ${clientException.message}"
                    serviceException != null -> "服务端异常: ${serviceException.errorCode} - ${serviceException.message}"
                    else -> "未知错误"
                }
                callback.onFailure(msg)
            }
        })
    }

    /**
     * 批量上传文件到阿里云 OSS
     * @param bucketName Bucket 名称
     * @param objectKeys OSS 中的 Key 列表（含路径），与 localFilePaths 一一对应
     * @param localFilePaths 本地文件绝对路径列表，与 objectKeys 一一对应
     * @param callback 批量上传回调（整体进度 / 每个文件结果 / 全部完成）
     */
    fun batchUploadToOss(bucketName: String,objectKeys: List<String>,localFilePaths: List<String>,callback: OssBatchUploadCallback) {
        val client = ossClient
        if (client == null) {
            callback.onAllFailed("OSSClient 未初始化，请先调用 initOss() 或 initOssWithSts()")
            return
        }
        if (objectKeys.size != localFilePaths.size || objectKeys.isEmpty()) {
            callback.onAllFailed("objectKeys 与 localFilePaths 数量不匹配或为空")
            return
        }

        log("OSS_DEBUG", "batchUpload bucketName=$bucketName, endpoint=$ossEndpoint")
        for (i in objectKeys.indices) {
            log("OSS_DEBUG", "batchUpload[$i] objectKey=${objectKeys[i]}, localFile=${localFilePaths[i]}")
        }

        val totalCount = objectKeys.size
        val completedCount = AtomicInteger(0)
        val failedCount = AtomicInteger(0)
        // 记录每个文件的上传结果
        val results = arrayOfNulls<BatchFileResult>(totalCount)

        callback.onBatchStart(totalCount)

        for (i in objectKeys.indices) {
            val objectKey = objectKeys[i]
            val localFilePath = localFilePaths[i]
            val index = i

            val file = File(localFilePath)
            if (!file.exists()) {
                results[index] = BatchFileResult(objectKey, localFilePath, false, null, "文件不存在")
                val failed = failedCount.incrementAndGet()
                val completed = completedCount.incrementAndGet()
                callback.onFileFailed(index, objectKey, "文件不存在: $localFilePath")
                callback.onOverallProgress(completed, totalCount)
                if (completed == totalCount) {
                    callback.onBatchComplete(results.filterNotNull())
                }
                continue
            }

            val putRequest = PutObjectRequest(bucketName, objectKey, localFilePath)
            putRequest.progressCallback = OSSProgressCallback { _, currentSize, totalSize -> callback.onFileProgress(index, objectKey, currentSize, totalSize) }

            client.asyncPutObject(putRequest, object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                override fun onSuccess(request: PutObjectRequest, result: PutObjectResult) {
                    val url = "${ossEndpoint}/${bucketName}/${objectKey}"
                    results[index] = BatchFileResult(objectKey, localFilePath, true, url, null)
                    callback.onFileSuccess(index, objectKey, url)
                    val completed = completedCount.incrementAndGet()
                    callback.onOverallProgress(completed, totalCount)
                    if (completed == totalCount) {
                        callback.onBatchComplete(results.filterNotNull())
                    }
                }

                override fun onFailure(request: PutObjectRequest, clientException: ClientException?, serviceException: ServiceException?) {
                    val msg = when {
                        clientException != null -> "客户端异常: ${clientException.message}"
                        serviceException != null -> {
                            log("OSS_DEBUG", "ServiceException: code=${serviceException.errorCode}, msg=${serviceException.message}, requestId=${serviceException.requestId}")
                            "服务端异常: ${serviceException.errorCode} - ${serviceException.message}"
                        }
                        else -> "未知错误"
                    }
                    results[index] = BatchFileResult(objectKey, localFilePath, false, null, msg)
                    failedCount.incrementAndGet()
                    callback.onFileFailed(index, objectKey, msg)
                    val completed = completedCount.incrementAndGet()
                    callback.onOverallProgress(completed, totalCount)
                    if (completed == totalCount) {
                        callback.onBatchComplete(results.filterNotNull())
                    }
                }
            })
        }
    }

    /**
     * 批量上传时单个文件的上传结果
     */
    data class BatchFileResult(
        val objectKey: String,
        val localFilePath: String,
        val success: Boolean,
        val objectUrl: String?,
        val errorMsg: String?
    )

    /**
     * OSS 批量上传回调接口
     */
    interface OssBatchUploadCallback {
        /** 批量上传开始 @param totalCount 总文件数 */
        fun onBatchStart(totalCount: Int)
        /** 单个文件上传进度 @param index 文件索引 @param objectKey 文件Key @param currentSize 已上传字节数 @param totalSize 文件总字节数 */
        fun onFileProgress(index: Int, objectKey: String, currentSize: Long, totalSize: Long)
        /** 单个文件上传成功 @param index 文件索引 @param objectKey 文件Key @param objectUrl 文件访问URL */
        fun onFileSuccess(index: Int, objectKey: String, objectUrl: String)
        /** 单个文件上传失败 @param index 文件索引 @param objectKey 文件Key @param errorMsg 错误信息 */
        fun onFileFailed(index: Int, objectKey: String, errorMsg: String)
        /** 整体进度回调 @param completedCount 已完成（成功+失败）的文件数 @param totalCount 总文件数 */
        fun onOverallProgress(completedCount: Int, totalCount: Int)
        /** 全部文件处理完成 @param results 每个文件的上传结果列表 */
        fun onBatchComplete(results: List<BatchFileResult>)
        /** 批量上传整体失败（如参数错误、OSSClient未初始化等） @param errorMsg 错误信息 */
        fun onAllFailed(errorMsg: String)
    }

    /**
     * 从阿里云 OSS 下载文件
     * @param bucketName Bucket 名称
     * @param objectKey 文件在 OSS 中的 Key（含路径），如 "images/photo_123.jpg"
     * @param localSavePath 下载后保存的本地文件绝对路径
     * @param callback 下载回调（进度 / 成功 / 失败）
     */
    fun downloadFromOss(
        bucketName: String,
        objectKey: String,
        localSavePath: String,
        callback: OssDownloadCallback
    ) {
        val client = ossClient
        if (client == null) {
            callback.onFailure("OSSClient 未初始化，请先调用 initOss() 或 initOssWithSts()")
            return
        }

        val getRequest = GetObjectRequest(bucketName, objectKey)

        client.asyncGetObject(getRequest, object : OSSCompletedCallback<GetObjectRequest, GetObjectResult> {
            override fun onSuccess(request: GetObjectRequest, result: GetObjectResult) {
                MainScope().launch(Dispatchers.IO) {
                    try {
                        val inputStream = result.objectContent
                        val totalSize = result.contentLength
                        // 确保父目录存在
                        val outputFile = File(localSavePath)
                        outputFile.parentFile?.mkdirs()

                        FileOutputStream(outputFile).use { fos ->
                            val buffer = ByteArray(8 * 1024)
                            var downloadedSize = 0L
                            var len: Int
                            while (inputStream.read(buffer).also { len = it } != -1) {
                                fos.write(buffer, 0, len)
                                downloadedSize += len
                                callback.onProgress(downloadedSize, totalSize)
                            }
                            fos.flush()
                        }
                        inputStream.close()
                        callback.onSuccess(localSavePath)
                    } catch (e: Exception) {
                        callback.onFailure("写入文件失败: ${e.message}")
                    }
                }
            }

            override fun onFailure(request: GetObjectRequest, clientException: ClientException?, serviceException: ServiceException?) {
                val msg = when {
                    clientException != null -> "客户端异常: ${clientException.message}"
                    serviceException != null -> "服务端异常: ${serviceException.errorCode} - ${serviceException.message}"
                    else -> "未知错误"
                }
                callback.onFailure(msg)
            }
        })
    }

    /**
     * 释放 OSSClient 资源
     */
    fun shutdownOss() {
        ossClient = null
    }

    /**
     * OSS 上传回调接口
     */
    interface OssUploadCallback {
        /** 上传进度回调 @param currentSize 已上传字节数 @param totalSize 文件总字节数 */
        fun onProgress(currentSize: Long, totalSize: Long)
        /** 上传成功 @param objectUrl 文件在 OSS 中的访问 URL */
        fun onSuccess(objectUrl: String)
        /** 上传失败 @param errorMsg 错误信息 */
        fun onFailure(errorMsg: String)
    }

    /**
     * OSS 下载回调接口
     */
    interface OssDownloadCallback {
        /** 下载进度回调 @param downloadedSize 已下载字节数 @param totalSize 文件总字节数 */
        fun onProgress(downloadedSize: Long, totalSize: Long)
        /** 下载成功 @param localFilePath 保存到本地的文件路径 */
        fun onSuccess(localFilePath: String)
        /** 下载失败 @param errorMsg 错误信息 */
        fun onFailure(errorMsg: String)
    }

}
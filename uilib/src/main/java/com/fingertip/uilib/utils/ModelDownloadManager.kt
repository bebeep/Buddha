package com.fingertip.uilib.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 3D模型文件下载管理器
 *
 * 功能：
 * - 从服务器下载3D模型文件(.glb)到本地
 * - 本地缓存管理，避免重复下载
 * - 基于文件版本号(ETag/Last-Modified)判断是否需要更新
 * - 下载进度回调
 */
object ModelDownloadManager {

    private const val TAG = "ModelDownloadManager"
    private const val SP_NAME = "model_download_prefs"
    private const val KEY_MODEL_VERSION = "model_version_"
    private const val KEY_MODEL_PATH = "model_path_"

    /** 默认测试模型地址 */
    const val DEFAULT_MODEL_URL = "https://b-buddha.oss-cn-beijing.aliyuncs.com/res/bee.glb"

    /** 模拟的模型版本号（后续替换为真实接口返回） */
    private const val MOCK_MODEL_VERSION = "v1"

    private val httpClient = OkHttpClient()

    /**
     * 模型下载回调
     */
    interface ModelDownloadCallback {
        /** 下载进度 0.0 ~ 1.0 */
        fun onProgress(progress: Float)

        /** 下载完成，返回本地文件路径 */
        fun onSuccess(localFilePath: String)

        /** 下载失败 */
        fun onError(errorMsg: String)
    }

    /**
     * 获取或下载模型文件
     *
     * 流程：
     * 1. 检查本地是否已有缓存
     * 2. 如有缓存，对比版本号判断是否需要更新
     * 3. 如无缓存或需要更新，从服务器下载
     *
     * @param context 上下文
     * @param modelUrl 模型下载地址
     * @param modelId 模型唯一标识（用于区分不同模型的缓存）
     * @param callback 下载回调
     */
    fun getOrDownloadModel(
        context: Context,
        modelUrl: String = DEFAULT_MODEL_URL,
        modelId: String = "worship_model",
        callback: ModelDownloadCallback? = null
    ) {
        val sp = getSp(context)
        val localDir = getModelDir(context)
        val fileName = getFileNameFromUrl(modelUrl)
        val localFile = File(localDir, fileName)

        // 检查本地缓存是否存在
        if (localFile.exists()) {
            val cachedVersion = sp.getString(KEY_MODEL_VERSION + modelId, "") ?: ""
            val serverVersion = getServerVersion(modelId)

            if (cachedVersion == serverVersion) {
                // 版本一致，直接使用本地缓存
                Log.d(TAG, "使用本地缓存: ${localFile.absolutePath}")
                callback?.onProgress(1.0f)
                callback?.onSuccess(localFile.absolutePath)
                return
            } else {
                // 版本不一致，需要重新下载
                Log.d(TAG, "版本更新: $cachedVersion -> $serverVersion，重新下载")
            }
        }

        // 开始下载
        downloadModel(context, modelUrl, modelId, localFile, callback)
    }

    /**
     * 强制重新下载模型（不管本地是否有缓存）
     */
    fun forceDownloadModel(
        context: Context,
        modelUrl: String = DEFAULT_MODEL_URL,
        modelId: String = "worship_model",
        callback: ModelDownloadCallback? = null
    ) {
        val localDir = getModelDir(context)
        val fileName = getFileNameFromUrl(modelUrl)
        val localFile = File(localDir, fileName)

        // 删除旧文件
        if (localFile.exists()) {
            localFile.delete()
        }

        downloadModel(context, modelUrl, modelId, localFile, callback)
    }

    /**
     * 执行下载
     */
    private fun downloadModel(
        context: Context,
        modelUrl: String,
        modelId: String,
        localFile: File,
        callback: ModelDownloadCallback?
    ) {
        // 确保目录存在
        localFile.parentFile?.mkdirs()

        val request = Request.Builder()
            .url(modelUrl)
            .build()

        Log.d(TAG, "开始下载模型: $modelUrl")

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "下载失败", e)
                callback?.onError("下载失败: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback?.onError("服务器返回错误: ${response.code}")
                    return
                }

                val body = response.body
                if (body == null) {
                    callback?.onError("响应体为空")
                    return
                }

                val contentLength = body.contentLength()
                var bytesRead = 0L

                try {
                    FileOutputStream(localFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        body.byteStream().apply {
                            while (read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                                bytesRead += read
                                if (contentLength > 0) {
                                    val progress = bytesRead.toFloat() / contentLength
                                    callback?.onProgress(progress)
                                }
                            }
                        }
                        fos.flush()
                    }

                    // 保存版本信息（使用服务端版本号，与 getServerVersion 保持一致）
                    val serverVersion = getServerVersion(modelId)
                    getSp(context).edit()
                        .putString(KEY_MODEL_VERSION + modelId, serverVersion)
                        .putString(KEY_MODEL_PATH + modelId, localFile.absolutePath)
                        .apply()

                    Log.d(TAG, "下载完成: ${localFile.absolutePath}, 版本: $serverVersion")
                    callback?.onSuccess(localFile.absolutePath)

                } catch (e: IOException) {
                    Log.e(TAG, "写入文件失败", e)
                    // 下载失败，删除不完整的文件
                    if (localFile.exists()) {
                        localFile.delete()
                    }
                    callback?.onError("文件写入失败: ${e.message}")
                }
            }
        })
    }

    /**
     * 获取本地已缓存的模型文件路径（如果存在）
     */
    fun getCachedModelPath(context: Context, modelUrl: String = DEFAULT_MODEL_URL): String? {
        val localDir = getModelDir(context)
        val fileName = getFileNameFromUrl(modelUrl)
        val localFile = File(localDir, fileName)
        return if (localFile.exists()) localFile.absolutePath else null
    }

    /**
     * 清除指定模型的缓存
     */
    fun clearCache(context: Context, modelId: String = "worship_model") {
        val sp = getSp(context)
        val path = sp.getString(KEY_MODEL_PATH + modelId, null)
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        sp.edit()
            .remove(KEY_MODEL_VERSION + modelId)
            .remove(KEY_MODEL_PATH + modelId)
            .apply()
    }

    /**
     * 模拟从服务端获取模型版本号
     * TODO: 后续替换为真实接口调用
     */
    private fun getServerVersion(modelId: String): String {
        // 模拟接口返回，实际应从服务器获取
        // 预设参数: modelId -> 返回对应版本
        return MOCK_MODEL_VERSION
    }



    /**
     * 从URL提取文件名
     */
    private fun getFileNameFromUrl(url: String): String {
        val lastSlash = url.lastIndexOf('/')
        return if (lastSlash >= 0) url.substring(lastSlash + 1) else "model.glb"
    }

    /**
     * 获取模型存储目录
     */
    private fun getModelDir(context: Context): File {
        return File(context.filesDir, "3d_models")
    }

    /**
     * 获取SharedPreferences
     */
    private fun getSp(context: Context): SharedPreferences {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }
}

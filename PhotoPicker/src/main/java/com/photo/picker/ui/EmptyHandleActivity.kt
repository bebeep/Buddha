package com.photo.picker.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.photo.picker.GalleryPickerHelper
import com.photo.picker.MediaData
import com.photo.picker.MimeType
import com.photo.picker.MediaType
import com.photo.picker.callback.MediaResultCallback
import com.photo.picker.lifecycle.LifecycleObserverHelper
import com.photo.picker.luban.CompressResult
import com.photo.picker.luban.Luban
import com.photo.picker.utils.Utils
import kotlinx.coroutines.launch
import java.io.File

/**
 * <pre>
 *     类描述  : 空处理页面
 *     负责注册 ActivityResultLauncher 并启动对应的媒体选择/拍照/拍摄操作
 *     对于相机拍照/拍摄，直接在此处理结果，避免进程被杀后状态丢失
 *
 *     @author : never
 *     @since   : 2024/10/10
 * </pre>
 */
class EmptyHandleActivity : ComponentActivity() {

    private var photoFilePath: String = ""
    private var captureVideoFilePath: String = ""

    companion object {
        const val EXTRA_MEDIA_TYPE = "media_type"
        const val EXTRA_PHOTO_PATH = "photo_path"
        const val EXTRA_CAPTURE_VIDEO_PATH = "capture_video_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val mLifecycleObserver = LifecycleObserverHelper.getObserver()
        if (mLifecycleObserver != null) {
            lifecycle.addObserver(mLifecycleObserver)
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 恢复被杀前的状态
        savedInstanceState?.let { bundle ->
            photoFilePath = bundle.getString(EXTRA_PHOTO_PATH, "")
            captureVideoFilePath = bundle.getString(EXTRA_CAPTURE_VIDEO_PATH, "")
        }

        val mediaTypeOrdinal = intent.getIntExtra(EXTRA_MEDIA_TYPE, -1)
        if (mediaTypeOrdinal < 0) return

        val mediaType = MediaType.values()[mediaTypeOrdinal]

        // 相机拍照/拍摄：直接在此注册launcher并处理结果，不依赖observer
        if (mediaType == MediaType.CAMERA) {
            handleCameraPhoto()
            return
        }
        if (mediaType == MediaType.CAPTURE_VIDEO) {
            handleCameraVideo()
            return
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_PHOTO_PATH, photoFilePath)
        outState.putString(EXTRA_CAPTURE_VIDEO_PATH, captureVideoFilePath)
    }

    private fun handleCameraPhoto() {
        val launcher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (!success) {
                GalleryPickerHelper.getCallback()?.onCancel()
                finish()
                return@registerForActivityResult
            }
            if (photoFilePath.isNotEmpty()) {
                lifecycleScope.launch {
                    val file = File(photoFilePath)
                    if (file.exists()) {
                        val compress = GalleryPickerHelper.getIsCompress()
                        if (compress) {
                            val result = Luban(this@EmptyHandleActivity).load(file)
                                .ignoreSize(GalleryPickerHelper.getIgnoreSize())
                                .quality(GalleryPickerHelper.getQuality())
                                .keepAlpha(false).keepSize(false).single()
                            handleCompressResult(result)
                        } else {
                            callbackWithImage(file.path)
                        }
                    } else {
                        GalleryPickerHelper.getCallback()?.onCancel()
                        finish()
                    }
                }
            } else {
                GalleryPickerHelper.getCallback()?.onCancel()
                finish()
            }
        }

        // 如果路径已恢复，直接launch；否则创建新文件
        if (photoFilePath.isNotEmpty()) {
            val file = File(photoFilePath)
            if (file.exists()) {
                launchCamera(launcher, file)
                return
            }
        }

        lifecycleScope.launch {
            val file = Utils.createCameraFile(this@EmptyHandleActivity)
            photoFilePath = file.path
            launchCamera(launcher, file)
        }
    }

    private fun launchCamera(launcher: ActivityResultLauncher<Uri>, file: File) {
        val fileUri = FileProvider.getUriForFile(
            this, "${applicationContext.packageName}.fileprovider", file
        )
        launcher.launch(fileUri)
    }

    private fun handleCameraVideo() {
        val launcher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
            if (!success) {
                GalleryPickerHelper.getCallback()?.onCancel()
                finish()
                return@registerForActivityResult
            }
            if (captureVideoFilePath.isNotEmpty()) {
                lifecycleScope.launch {
                    val file = File(captureVideoFilePath)
                    if (file.exists()) {
                        val mediaFiles = mutableListOf<MediaData>()
                        mediaFiles.add(MediaData(MimeType.VIDEO, file.path))
                        GalleryPickerHelper.getCallback()?.onMediaResult(mediaFiles)
                        finish()
                    } else {
                        GalleryPickerHelper.getCallback()?.onCancel()
                        finish()
                    }
                }
            } else {
                GalleryPickerHelper.getCallback()?.onCancel()
                finish()
            }
        }

        // 如果路径已恢复，直接launch；否则创建新文件
        if (captureVideoFilePath.isNotEmpty()) {
            val file = File(captureVideoFilePath)
            if (file.exists()) {
                launchVideo(launcher, file)
                return
            }
        }

        lifecycleScope.launch {
            val file = Utils.createCaptureVideoFile(this@EmptyHandleActivity)
            captureVideoFilePath = file.path
            launchVideo(launcher, file)
        }
    }

    private fun launchVideo(launcher: ActivityResultLauncher<Uri>, file: File) {
        val fileUri = FileProvider.getUriForFile(
            this, "${applicationContext.packageName}.fileprovider", file
        )
        launcher.launch(fileUri)
    }

    private fun handleCompressResult(result: CompressResult) {
        when (result) {
            is CompressResult.Success -> {
                callbackWithImage(result.file?.path ?: "")
            }
            is CompressResult.Error -> {
                GalleryPickerHelper.getCallback()?.onCancel()
                finish()
            }
            else -> {}
        }
    }

    private fun callbackWithImage(filePath: String) {
        if (filePath.isEmpty()) {
            GalleryPickerHelper.getCallback()?.onCancel()
            finish()
            return
        }
        val list = mutableListOf(filePath)
        val mediaFiles = mutableListOf(MediaData(MimeType.IMAGE, filePath))
        GalleryPickerHelper.getCallback()?.onResult(list)
        GalleryPickerHelper.getCallback()?.onMediaResult(mediaFiles)
        finish()
    }
}

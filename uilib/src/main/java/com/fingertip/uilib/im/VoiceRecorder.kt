package com.fingertip.uilib.im

import android.content.Context
import android.media.MediaRecorder
import java.io.File

/**
 * 语音消息录制（AAC/m4a，IM createSoundMessage 直接可用）
 *
 * 简单两态：start 录音，stop 返回文件+时长。cancel 丢弃。
 * 调用方负责 RECORD_AUDIO 运行时权限。
 */
class VoiceRecorder(context: Context) {

    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var filePath: String? = null
    private var startAt = 0L

    private val dir: File
        get() = File(appContext.cacheDir, "im_voice").apply { if (!exists()) mkdirs() }

    fun start(): String? {
        return try {
            val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            val r = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(appContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(16000)
            r.setAudioChannels(1)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            filePath = file.absolutePath
            startAt = System.currentTimeMillis()
            file.absolutePath
        } catch (e: Exception) {
            release()
            null
        }
    }

    /** 停止并返回 (文件路径, 秒)。最短 1 秒，失败返回 null */
    fun stop(): Pair<String, Int>? {
        return try {
            recorder?.stop()
            release()
            val path = filePath ?: return null
            val seconds = ((System.currentTimeMillis() - startAt) / 1000).toInt().coerceAtLeast(1)
            path to seconds
        } catch (_: Exception) {
            release()
            null
        }
    }

    fun cancel() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        release()
        filePath?.let { File(it).delete() }
        filePath = null
    }

    private fun release() {
        recorder?.release()
        recorder = null
    }
}

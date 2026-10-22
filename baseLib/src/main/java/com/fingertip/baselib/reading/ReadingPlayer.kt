package com.fingertip.baselib.reading

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer

/**
 * 读音播放。
 * 音频按词条一条一文件，打包在 assets/reading/audio 下，不联网。
 * 播放器全 App 只有一个，点新的先把旧的停掉
 */
object ReadingPlayer {

    private const val DIR = "reading/audio/"

    private var player: MediaPlayer? = null

    fun play(context: Context, audio: String) {
        stop()
        if (audio.isEmpty()) return
        //只读 assets，用 application 的 context，别把页面攥在手里
        val ctx = context.applicationContext
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            val path = DIR + audio
            //assets 里的 mp3 通常不会被压缩，能拿到 fd 就直接读；拿不到就整条读进内存再播
            val fd = try {
                ctx.assets.openFd(path)
            } catch (e: Exception) {
                null
            }
            if (fd != null) {
                fd.use {
                    mp.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
            } else {
                mp.setDataSource(ByteSource(ctx.assets.open(path).use { it.readBytes() }))
            }
            mp.setOnCompletionListener { finish(it) }
            mp.setOnErrorListener { p, _, _ ->
                finish(p)
                true
            }
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            //音频坏了或没打进包，静默跳过，不影响查读音
            release(mp)
        }
    }

    fun stop() {
        val mp = player ?: return
        player = null
        try {
            mp.stop()
        } catch (e: Exception) {
            //没在播的时候 stop 会抛，忽略
        }
        release(mp)
    }

    /**
     * 播完或出错时收尾。要认一下是不是当前这个，
     * 否则连点两下会把新起的那个一起收掉
     */
    private fun finish(mp: MediaPlayer) {
        if (player === mp) {
            player = null
        }
        release(mp)
    }

    private fun release(mp: MediaPlayer) {
        try {
            mp.release()
        } catch (e: Exception) {
            //已经释放过就算了
        }
    }

    /**
     * 兜底数据源：整条音频放在内存里供给 MediaPlayer
     */
    private class ByteSource(private val data: ByteArray) : MediaDataSource() {

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= data.size) return -1
            val len = minOf(size.toLong(), data.size - position).toInt()
            System.arraycopy(data, position.toInt(), buffer, offset, len)
            return len
        }

        override fun getSize(): Long = data.size.toLong()

        override fun close() {
        }
    }
}

package com.fingertip.uilib.view.nianfo

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build

/**
 * 木鱼敲击音：无素材，AudioTrack 静态模式实时合成
 * （对应原型 Web Audio woodSound：三角波频率下滑 + 快起指数衰减包络）。
 * 净念（perfect）音高略高。敲击频率低（人手 ≤5 次/秒），每次新建播完即释放，实现最省心。
 */
class NianfoWoodSound {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val DURATION_SEC = 0.22f
        private const val PEAK = 0.7f
    }

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    /** 敲一下。perfect=true 音高 440，否则 380（原型同款） */
    fun hit(perfect: Boolean) {
        try {
            val f0 = if (perfect) 440f else 380f
            val n = (SAMPLE_RATE * DURATION_SEC).toInt()
            val pcm = ShortArray(n)
            var phase = 0f
            for (i in 0 until n) {
                val t = i / SAMPLE_RATE.toFloat()
                //频率指数下滑：0.11s 内从 f0 滑到 150Hz
                val k = minOf(t / 0.11f, 1f)
                val freq = f0 * Math.pow(150.0 / f0, k.toDouble()).toFloat()
                phase += 2f * Math.PI.toFloat() * freq / SAMPLE_RATE
                //包络：快起 4ms 到峰值，指数衰减到 0.2s
                val env = if (t < 0.004f) {
                    t / 0.004f
                } else {
                    Math.pow(Math.E.toDouble(), -((t - 0.004) / 0.045)).toFloat() * PEAK
                }
                pcm[i] = (Math.sin(phase.toDouble()).toFloat() * env * Short.MAX_VALUE).toInt().toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            track.write(pcm, 0, pcm.size)
            //播完自释放
            track.setNotificationMarkerPosition(n)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) { t?.release() }
                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
            track.play()
        } catch (_: Exception) {
            //音频失败静默，不影响敲击
        }
    }
}

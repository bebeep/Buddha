package com.fingertip.uilib.fragment.moment

import VideoHandle.EpEditor
import VideoHandle.EpVideo
import VideoHandle.OnEditorListener
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import com.blankj.utilcode.util.ThreadUtils.runOnUiThread
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopFragment
import com.fingertip.uilib.R
import kotlinx.android.synthetic.main.fragment_handle_video.*
import test.taq.com.cropvideolibrary.interfaces.VideoTrimListener
import java.io.File


/**
 * 处理视频
 */
class HandleVideoFragment: TopFragment(), VideoTrimListener {
    override fun layoutId() = R.layout.fragment_handle_video
    var path: String? = null
    override fun initShiTu() {
        log("HandleVideoFragment", "视频裁剪： $path")
        trimmerView.setOnTrimVideoListener(this)
        trimmerView.initVideoByURI(Uri.parse(path))
    }


    private fun Cilp(start: Float, duration: Float) {
        val file = File(Environment.getExternalStorageDirectory().path,"/22m_tiny_video/")
        if (!file.exists()) file.mkdirs()
        log("HandleVideoFragment", "视频裁剪： $start-$duration")
        val epVideo = EpVideo(path)
        epVideo.clip(start, duration)

        //输出选项，参数为输出文件路径(目前仅支持mp4格式输出)
        val outFile = Environment.getExternalStorageDirectory().absolutePath + "/22m_tiny_video/video_" + System.currentTimeMillis() + ".mp4"
        val outputOption = EpEditor.OutputOption(outFile)
        outputOption.frameRate = 24 //输出视频帧率,默认30
        outputOption.bitRate = 2 //输出视频码率,默认10
        EpEditor.exec(epVideo, outputOption, object : OnEditorListener {
            override fun onSuccess() {
                isTrimming = false
                log("HandleVideoFragment", "视频裁剪EpEditor： onSuccess")
                onFinishTrim(outFile)
            }

            override fun onFailure() {
                runOnUiThread {
                    try {
                        File(outFile).delete()
                    } catch (e: Exception) {
                        log("HandleVideoFragment", "视频裁剪EpEditor： onFailed   ${e.message}")
                    }
                }
            }

            override fun onProgress(v: Float) {
                log("HandleVideoFragment", "视频裁剪EpEditor： onProgress:$v")
            }
        })
    }




    private var isTrimming = false
    override fun onStartTrim(start: Float, duration: Float) {
        log("HandleVideoFragment", "isTrimming===============================$isTrimming $duration")
        if (isTrimming) return
        isTrimming = true
        trimmerView.onVideoPause()
        trimmerView.setRestoreState(true)
        startWaiting()
        Cilp(start, duration)
    }

    override fun onFinishTrim(url: String?) {
        trimmerView.post { loadEnding() }
        setFragmentResult(RESULT_OK, Bundle().apply {
            putString("video-file-path", url)
        })
        pop()
    }


    override fun onCancel() {
        pop()
    }

    override fun onProgress(Progress: String?) {}


    override fun onPause() {
        super.onPause()
        trimmerView.onVideoPause()
        trimmerView.setRestoreState(true)
    }
    

    override fun onStop() {
        loadEnding()
        super.onStop()
    }

    override fun onDestroy() {
        trimmerView?.onDestroy()
        super.onDestroy()
    }

}
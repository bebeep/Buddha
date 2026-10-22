package com.fingertip.uilib.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.View
import com.fingertip.baselib.reading.ReadingEntry
import com.fingertip.baselib.reading.ReadingPlayer
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogFojingReadingBinding

/**
 * 点字查读音
 * 底部弹一层，不挡着正在读的正文；点一下收起来。
 * 有录音的词多给一个播放按钮，没有的就只显示拼音
 */
class FojingReadingDialog(context: Context, private val entry: ReadingEntry) :
    TopDialogBottomSheetDialog(context) {

    private lateinit var binding: DialogFojingReadingBinding

    override fun getLayoutId() = R.layout.dialog_fojing_reading

    override fun onViewCreate(view: View) {
        binding = DialogFojingReadingBinding.bind(view)
        binding.tvWord.text = entry.word
        binding.tvReading.text = entry.reading
        if (entry.kind.isEmpty()) {
            binding.tvKind.visibility = View.GONE
        } else {
            binding.tvKind.text = entry.kind
        }

        if (entry.audio.isEmpty()) {
            binding.tvPlay.visibility = View.GONE
        } else {
            binding.tvPlay.setOnClickListener { ReadingPlayer.play(context, entry.audio) }
            binding.tvTip.text = "点「听读音」听发音，点其他处关闭"
        }

        view.setOnClickListener { dismiss() }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        //弹窗收了就别再念了
        ReadingPlayer.stop()
    }
}

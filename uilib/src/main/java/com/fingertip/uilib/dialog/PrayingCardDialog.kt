package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.PrayingBean
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogPrayingCardBinding

/**
 * 拆福卡详情弹窗，对应原型「祈福-拆福卡」
 * 展示福卡内容与祝福者，可送出祝福；祝福成功/失败由页面回调
 */
class PrayingCardDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    var onBless: ((PrayingBean) -> Unit)? = null

    private lateinit var binding: DialogPrayingCardBinding
    private var item: PrayingBean? = null

    override fun getLayoutId(): Int = R.layout.dialog_praying_card

    override fun onViewCreate(view: View) {
        binding = DialogPrayingCardBinding.bind(view)
        binding.tvBless.setOnClickListener {
            val praying = item ?: return@setOnClickListener
            if (praying.myBlessed) return@setOnClickListener
            //置灰防连点，失败恢复
            binding.tvBless.alpha = 0.6f
            onBless?.invoke(praying)
        }
    }

    fun render(praying: PrayingBean) {
        item = praying
        binding.tvContent.text = praying.content
        binding.tvBlessCount.text = "${praying.blessCount}人已祝福"
        val names = praying.blessUserNames.orEmpty()
        binding.tvBlessers.text = if (names.isEmpty()) "虚位以待，等你送上祝福"
        else names.joinToString("、").let { if (it.length > 20) it.take(20) + "…等" else it }

        if (praying.myBlessed) {
            binding.tvBless.text = "已祝福"
            binding.tvBless.alpha = 0.5f
            binding.tvBless.isEnabled = false
        }
    }

    fun onBlessSuccess() {
        val praying = item ?: return
        praying.myBlessed = true
        praying.blessCount += 1
        binding.tvBlessCount.text = "${praying.blessCount}人已祝福"
        binding.tvBless.text = "已祝福"
        binding.tvBless.alpha = 0.5f
        binding.tvBless.isEnabled = false
    }

    fun onBlessFailed(errorMsg: String?) {
        binding.tvBless.alpha = 1f
    }
}

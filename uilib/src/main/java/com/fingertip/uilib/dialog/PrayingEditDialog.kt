package com.fingertip.uilib.dialog

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogPrayingEditBinding

/**
 * 编辑祈福弹窗，对应原型「祈福-编辑祈福」
 * 内容限20字；封面：默认免费 / 祈福卡付费5功德币（服务端扣费）
 */
class PrayingEditDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    companion object {
        const val COVER_DEFAULT = 0
        const val COVER_PAID = 1
    }

    //点「开始祈福」，发布交给外部（页面统一走 VM）
    var onSubmit: ((content: String, coverType: Int) -> Unit)? = null

    private lateinit var binding: DialogPrayingEditBinding
    private var coverType = COVER_DEFAULT

    override fun getLayoutId(): Int = R.layout.dialog_praying_edit

    override fun onViewCreate(view: View) {
        binding = DialogPrayingEditBinding.bind(view)

        binding.etPrayingContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.tvContentCount.text = "${s?.length ?: 0}/20"
            }
        })

        binding.tvCoverDefault.setOnClickListener { selectCover(COVER_DEFAULT) }
        //付费封面卡片：它的第二个子LinearLayout，点它选中付费封面
        (binding.tvCoverDefault.parent as? LinearLayout)?.getChildAt(1)?.setOnClickListener {
            selectCover(COVER_PAID)
            ToastUtil.showMessage("选择该封面祈福时将消耗5功德币")
        }

        binding.tvSubmit.setOnClickListener {
            val content = binding.etPrayingContent.text?.toString()?.trim().orEmpty()
            if (content.isEmpty()) {
                ToastUtil.showMessage("祈福内容不能为空")
                return@setOnClickListener
            }
            //提交中置灰防连点，失败时 onPostFailed 恢复
            binding.tvSubmit.alpha = 0.6f
            onSubmit?.invoke(content, coverType)
        }
    }

    private fun selectCover(type: Int) {
        coverType = type
        binding.tvCoverDefault.setBackgroundResource(
            if (type == COVER_DEFAULT) R.drawable.bg_praying_cover_selected
            else R.drawable.bg_praying_cover_normal
        )
        val parent = binding.tvCoverDefault.parent as? LinearLayout ?: return
        val paid = parent.getChildAt(1)
        paid.setBackgroundResource(
            if (type == COVER_PAID) R.drawable.bg_praying_cover_selected
            else R.drawable.bg_praying_cover_normal
        )
    }

    /**
     * 填充推荐标签，点一下填入输入框
     */
    fun renderTags(tags: List<String>?) {
        val container = binding.llTags
        container.removeAllViews()
        tags?.take(3)?.forEach { tag ->
            val tv = TextView(context)
            tv.text = tag
            tv.textSize = 12f
            tv.setTextColor(Color.parseColor("#FFA030"))
            tv.setBackgroundResource(R.drawable.bg_wish_tag)
            tv.setPadding(30, 12, 30, 12)
            tv.gravity = Gravity.CENTER
            tv.setOnClickListener { binding.etPrayingContent.setText(tag) }
            val lp = LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 20
            container.addView(tv, lp)
        }
    }

    fun onPostSuccess() {
        dismiss()
    }

    fun onPostFailed() {
        binding.tvSubmit.alpha = 1f
    }
}

package com.fingertip.uilib.dialog

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogWishEditBinding

/**
 * 编辑愿望底部弹窗，对应原型「许愿-编辑愿望」
 * 内容最多 500 字，推荐标签由外部传入（服务端 getWishingTag），点一下填入输入框
 */
class WishEditDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    //点「开始许愿」，发布交给外部（页面统一走 VM）
    var onSubmit: ((String) -> Unit)? = null

    private lateinit var binding: DialogWishEditBinding

    override fun getLayoutId(): Int = R.layout.dialog_wish_edit

    override fun onViewCreate(view: View) {
        binding = DialogWishEditBinding.bind(view)
        binding.etWishContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.tvContentCount.text = "${s?.length ?: 0}/500"
            }
        })
        binding.tvSubmit.setOnClickListener {
            val content = binding.etWishContent.text?.toString()?.trim().orEmpty()
            if (content.isEmpty()) {
                com.fingertip.baselib.util.ToastUtil.showMessage("愿望内容不能为空")
                return@setOnClickListener
            }
            //发布交给页面 VM，提交中置灰防连点，失败时 onPostFailed 恢复
            binding.tvSubmit.alpha = 0.6f
            onSubmit?.invoke(content)
        }
    }

    /**
     * 填充推荐标签
     */
    fun renderTags(tags: List<String>?) {
        val container = binding.llTags
        container.removeAllViews()
        tags?.forEach { tag ->
            val tv = TextView(context)
            tv.text = tag
            tv.textSize = 12f
            tv.setTextColor(Color.parseColor("#FFA030"))
            tv.setBackgroundResource(R.drawable.bg_wish_tag)
            tv.setPadding(30, 12, 30, 12)
            tv.gravity = Gravity.CENTER
            tv.setOnClickListener { binding.etWishContent.setText(tag) }
            val lp = LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 20
            container.addView(tv, lp)
        }
    }

    /**
     * 发布成功后关弹窗
     */
    fun onPostSuccess() {
        dismiss()
    }

    /**
     * 发布失败，恢复按钮可点
     */
    fun onPostFailed() {
        binding.tvSubmit.alpha = 1f
    }
}

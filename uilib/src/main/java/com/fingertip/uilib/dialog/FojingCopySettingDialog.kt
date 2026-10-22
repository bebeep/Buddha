package com.fingertip.uilib.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.ImageView
import com.fingertip.baselib.bean.BookCopySetting
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.baselib.view.MyTextView
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogFojingCopySettingBinding

/**
 * 抄经显示设置
 * 点一下立刻在抄经页生效（onPreview），关掉弹窗才把结果提交服务端（onSave），
 * 免得连点几下打一串请求
 */
class FojingCopySettingDialog(
    context: Context,
    private val setting: BookCopySetting,
    private val onPreview: (BookCopySetting) -> Unit,
    private val onSave: (BookCopySetting) -> Unit
) : TopDialogBottomSheetDialog(context) {

    private lateinit var binding: DialogFojingCopySettingBinding

    //改过才提交，只打开看一眼不发请求
    private var changed = false

    private val defaultColors get() = listOf(
        binding.ivDefaultColor1, binding.ivDefaultColor2,
        binding.ivDefaultColor3, binding.ivDefaultColor4
    )

    private val copyColors get() = listOf(
        binding.ivCopyColor1, binding.ivCopyColor2,
        binding.ivCopyColor3, binding.ivCopyColor4
    )

    private val bgColors get() = listOf(
        binding.ivBgColor1, binding.ivBgColor2, binding.ivBgColor3, binding.ivBgColor4
    )

    private val fontViews get() = listOf(
        binding.tvFont1, binding.tvFont2, binding.tvFont3, binding.tvFont4
    )

    override fun getLayoutId() = R.layout.dialog_fojing_copy_setting

    override fun onViewCreate(view: View) {
        binding = DialogFojingCopySettingBinding.bind(view)
        bindColor(defaultColors, setting.defaultColor) { setting.defaultColor = it }
        bindColor(copyColors, setting.copyColor) { setting.copyColor = it }
        bindColor(bgColors, setting.backgroundColor) { setting.backgroundColor = it }
        bindFont()
    }

    private fun bindColor(views: List<ImageView>, checked: Int, onPick: (Int) -> Unit) {
        applyColorChecked(views, checked)
        views.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                onPick(index + 1)
                applyColorChecked(views, index + 1)
                changed = true
                onPreview(setting)
            }
        }
    }

    /**
     * 选中的色块不透明，没选中的压暗，比原 selector 的选中态靠谱
     */
    private fun applyColorChecked(views: List<ImageView>, checked: Int) {
        views.forEachIndexed { index, imageView ->
            imageView.alpha = if (index + 1 == checked) 1f else 0.35f
        }
    }

    private fun bindFont() {
        applyFontChecked(fontViews, setting.fontSizeLevel)
        fontViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                setting.fontSizeLevel = index + 1
                applyFontChecked(fontViews, index + 1)
                changed = true
                onPreview(setting)
            }
        }
    }

    private fun applyFontChecked(views: List<MyTextView>, checked: Int) {
        views.forEachIndexed { index, view ->
            view.isSelected = index + 1 == checked
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        if (changed) onSave(setting)
    }
}

package com.fingertip.uilib.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.ImageView
import com.fingertip.baselib.bean.BookReadSetting
import com.fingertip.baselib.download.DownloadState
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.baselib.view.MyTextView
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogFojingReadSettingBinding

/**
 * 读经显示设置
 * 点一下立刻在阅读页生效（onPreview），关掉弹窗才把结果提交服务端（onSave），
 * 免得连点几下打一串请求被 RepeatSubmit 拦
 */
class FojingReadSettingDialog(
    context: Context,
    private val setting: BookReadSetting,
    private val downloadState: DownloadState?,
    private val onDownload: () -> Unit,
    private val onPreview: (BookReadSetting) -> Unit,
    private val onSave: (BookReadSetting) -> Unit
) : TopDialogBottomSheetDialog(context) {

    private lateinit var binding: DialogFojingReadSettingBinding

    //改过才提交，只打开看一眼不发请求
    private var changed = false

    private val textColors get() = listOf(
        binding.ivTextColor1, binding.ivTextColor2, binding.ivTextColor3, binding.ivTextColor4
    )

    private val bgColors get() = listOf(
        binding.ivBgColor1, binding.ivBgColor2, binding.ivBgColor3, binding.ivBgColor4
    )

    private val fontViews get() = listOf(
        binding.tvFont1, binding.tvFont2, binding.tvFont3, binding.tvFont4
    )

    private val lineViews get() = listOf(
        binding.tvLine1, binding.tvLine2, binding.tvLine3, binding.tvLine4
    )

    override fun getLayoutId() = R.layout.dialog_fojing_read_setting

    override fun onViewCreate(view: View) {
        binding = DialogFojingReadSettingBinding.bind(view)
        bindColor(textColors, setting.textColor) { setting.textColor = it }
        bindColor(bgColors, setting.backgroundColor) { setting.backgroundColor = it }
        bindFont()
        bindLine()
        bindKeep()
        bindTrad()
        bindOffline()
    }

    /**
     * 字形：简繁切换。简繁正文一字对一字，切换不会丢阅读位置
     */
    private fun bindTrad() {
        val button = binding.tvTradAction
        button.text = if (setting.useTrad()) "繁体" else "简体"
        button.setOnClickListener {
            setting.showTrad = if (setting.useTrad()) 0 else 1
            button.text = if (setting.useTrad()) "繁体" else "简体"
            changed = true
            onPreview(setting)
        }
    }

    /**
     * 离线整部：点一下开始下，下载中再点是停下来
     */
    private fun bindOffline() {
        renderOffline(downloadState)
        binding.tvOfflineAction.setOnClickListener { onDownload() }
    }

    /**
     * 下载进度由页面推过来，弹窗只管照着显示
     */
    fun updateDownload(state: DownloadState?) {
        renderOffline(state)
    }

    private fun renderOffline(state: DownloadState?) {
        //进度可能比视图先到，没绑完就先不管
        if (!this::binding.isInitialized) return
        val action = binding.tvOfflineAction
        if (state == null) {
            action.text = "下载整部"
            return
        }
        action.text = when {
            state.running -> "下载中 ${state.done}/${state.total} ${state.percent()}%"
            state.finished() -> "已缓存全部 ${state.total}章"
            else -> "下载整部"
        }
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
     * 选中的色块不透明，没选中的压暗
     */
    private fun applyColorChecked(views: List<ImageView>, checked: Int) {
        views.forEachIndexed { index, imageView ->
            imageView.alpha = if (index + 1 == checked) 1f else 0.35f
        }
    }

    private fun bindFont() {
        applyChecked(fontViews, setting.fontSizeLevel)
        fontViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                setting.fontSizeLevel = index + 1
                applyChecked(fontViews, index + 1)
                changed = true
                onPreview(setting)
            }
        }
    }

    private fun bindLine() {
        applyChecked(lineViews, setting.lineSpacingLevel)
        lineViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                setting.lineSpacingLevel = index + 1
                applyChecked(lineViews, index + 1)
                changed = true
                onPreview(setting)
            }
        }
    }

    private fun bindKeep() {
        val button = binding.tvKeepOn
        button.text = if (setting.keepOn()) "开" else "关"
        button.setOnClickListener {
            setting.keepScreenOn = if (setting.keepOn()) 0 else 1
            button.text = if (setting.keepOn()) "开" else "关"
            changed = true
            onPreview(setting)
        }
    }

    private fun applyChecked(views: List<MyTextView>, checked: Int) {
        views.forEachIndexed { index, view ->
            view.isSelected = index + 1 == checked
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        if (changed) onSave(setting)
    }
}

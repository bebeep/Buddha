package com.fingertip.uilib.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fingertip.baselib.top.TopDialog
import com.fingertip.baselib.view.MyTextView
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogActionSheetBinding

/**
 * 通用底部操作菜单
 *
 * 出现的场景都是「针对一条内容/一个人能做哪几件事」，比如动态详情的
 * 举报 / 拉黑，他人主页的举报 / 拉黑 / 取消拉黑。
 *
 * 为什么单独做一个而不是继续用弹窗按钮：审核要求举报和拉黑都要在**同一层**可达，
 * 塞进二次弹窗会让拉黑变成三级入口，判违规的风险比多写一个类高得多。
 *
 * @param title 面板标题，传 null 不显示
 * @param items 操作项，顺序即展示顺序；danger=true 的项文字标红（拉黑之类）
 */
class ActionSheetDialog(
    context: Context,
    private val title: String? = null,
    private val items: List<Item>
) : TopDialog(context) {

    /**
     * 一个操作项
     * @param text 文案
     * @param danger 是否为负向操作（标红）
     * @param onClick 点击回调，回调前弹窗已 dismiss
     */
    class Item(val text: String, val danger: Boolean = false, val onClick: () -> Unit)

    private lateinit var binding: DialogActionSheetBinding

    override fun attachLayoutRes(): Int = R.layout.dialog_action_sheet

    //面板贴底，需要整屏高度才能让遮罩铺满
    override fun IS_MATCH_PARENT(): Boolean = true

    override fun onViewInit(view: View?) {
        if (view == null) return
        binding = DialogActionSheetBinding.bind(view)
        setBottomDialog()
        window?.setDimAmount(0.5f)

        if (title.isNullOrEmpty()) {
            binding.tvTitle.visibility = View.GONE
        } else {
            binding.tvTitle.visibility = View.VISIBLE
            binding.tvTitle.text = title
        }

        val inflater = LayoutInflater.from(context)
        items.forEach { item ->
            //根节点本身就是 MyTextView，直接 cast，不用 findViewById
            val tv = inflater.inflate(R.layout.item_action_sheet, binding.llItems, false) as MyTextView
            tv.text = item.text
            if (item.danger) {
                tv.setTextColor(DANGER_COLOR)
            }
            tv.setOnClickListener {
                dismiss()
                item.onClick.invoke()
            }
            binding.llItems.addView(tv)
        }

        binding.tvCancel.setOnClickListener { dismiss() }
        binding.vOutside.setOnClickListener { dismiss() }

        //手势导航机型底部有一条 24dp 左右的系统条，不避让会把“取消”压住
        ViewCompat.setOnApplyWindowInsetsListener(binding.llPanel) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottom)
            insets
        }
    }

    companion object {
        /** 与举报弹窗里的危险色保持同一支红 */
        private val DANGER_COLOR = 0xFFB4453B.toInt()
    }
}

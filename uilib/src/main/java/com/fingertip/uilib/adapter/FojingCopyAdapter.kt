package com.fingertip.uilib.adapter

import android.content.Context
import android.util.TypedValue
import android.view.View
import com.fingertip.uilib.R
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.databinding.ItemFojingCopyBinding

/**
 * 抄经格：char 是这一格要抄的原文，sourceIndex 是它在正文字符串里的下标（上报进度用）
 */
class CopyCell(val char: String, val sourceIndex: Int) {
    //用户选进去的字，空表示还没抄
    var typed: String = ""

    //是不是当前正在抄的这一格
    var current: Boolean = false

    //跳过的格子，字还在但不计功德
    var skipped: Boolean = false
}

/**
 * 佛经-抄经
 * 上半格淡色列原文，下半格是用户从四个选项里选出来的字
 */
class FojingCopyAdapter(context: Context, val column: Int) :
    TopRcAdapter<CopyCell, TopRcAdapter.TopRcViewHolder>(context) {

    //抄经显示设置，由页面改，改完重绑一遍就生效
    var defaultTextColor: Int = 0x4D2C1900
    var copyTextColor: Int = 0xFF2C1900.toInt()
    var copyTextSize: Float = 30f

    //长按当前格（跳过）
    var onCellLongClick: ((CopyCell) -> Unit)? = null

    override fun initLayoutId(viewType: Int) = R.layout.item_fojing_copy

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemFojingCopyBinding>()
        val cell = get(position) ?: return
        binding.tvDefault.text = cell.char
        binding.tvDefault.setTextColor(defaultTextColor)
        binding.tvCopy.text = cell.typed
        binding.tvCopy.setTextSize(TypedValue.COMPLEX_UNIT_SP, copyTextSize)
        //跳过的格用浅色字，跟抄过的区分开
        binding.tvCopy.setTextColor(if (cell.skipped) 0xFFCCCCCC.toInt() else copyTextColor)
        //当前格换成红框米字格，一眼能看出抄到哪
        binding.tvCopy.setBackgroundResource(
            if (cell.current) R.drawable.bg_fojing_copy_answer
            else R.drawable.bg_fojing_copy_item
        )
        binding.vLineRight.visibility =
            if (position % column == column - 1 || position == data().size - 1) View.VISIBLE
            else View.GONE
        binding.root.setOnLongClickListener {
            if (cell.current) {
                onCellLongClick?.invoke(cell)
                true
            } else {
                false
            }
        }
    }
}

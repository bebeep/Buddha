package com.fingertip.uilib.dialog

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogWorshipBinding
import com.fingertip.uilib.viewmodel.WorshipVM

/**
 * 供佛弹窗：按价目选择供奉天数，确定后由页面调 worshipBuddha 接口
 */
class WorshipDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    //点「确定」，days/price 交给外部（页面统一走 VM）
    var onSubmit: ((days: Int, price: Int) -> Unit)? = null

    private lateinit var binding: DialogWorshipBinding
    private var prices: List<WorshipVM.PriceItem> = emptyList()
    private var selectedDays = 0

    override fun getLayoutId(): Int = R.layout.dialog_worship

    override fun onViewCreate(view: View) {
        binding = DialogWorshipBinding.bind(view)
        binding.ivClose.setOnClickListener { dismiss() }
        binding.tvSubmit.setOnClickListener {
            if (selectedDays == 0) {
                com.fingertip.baselib.util.ToastUtil.showMessage("请选择供奉时长")
                return@setOnClickListener
            }
            val price = prices.firstOrNull { it.days == selectedDays }?.price ?: 0
            binding.tvSubmit.alpha = 0.6f
            onSubmit?.invoke(selectedDays, price)
        }
    }

    /**
     * 填充佛像与价目
     */
    fun render(buddha: BuddhaConfig) {
        binding.tvBuddhaName.text = "供奉「${buddha.buddhaName ?: ""}」"
        binding.tvBalance.text = "我的功德币：${GlobalConfig.userData?.coin ?: 0}"
        prices = WorshipVM.parsePriceField(buddha.priceField)
        selectedDays = prices.firstOrNull()?.days ?: 0
        renderPrices()
    }

    private fun renderPrices() {
        val container = binding.llPrices
        container.removeAllViews()
        prices.forEach { item ->
            val tv = TextView(context)
            tv.text = "供奉${item.days}天 · ${item.price}功德币"
            tv.textSize = 14f
            tv.gravity = Gravity.CENTER
            tv.setPadding(0, 30, 0, 30)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 10
            container.addView(tv, lp)
            setSelectedStyle(tv, item.days == selectedDays)
            tv.setOnClickListener {
                selectedDays = item.days
                //刷新所有子项选中态
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i) as TextView
                    val day = prices.getOrNull(i)?.days ?: 0
                    setSelectedStyle(child, day == selectedDays)
                }
            }
        }
    }

    private fun setSelectedStyle(tv: TextView, selected: Boolean) {
        tv.setBackgroundResource(
            if (selected) R.drawable.bg_worship_price_selected else R.drawable.bg_worship_price_normal
        )
        tv.setTextColor(if (selected) Color.parseColor("#FFA030") else Color.parseColor("#666666"))
    }

    /**
     * 供佛成功后关弹窗
     */
    fun onWorshipSuccess() {
        dismiss()
    }

    /**
     * 供佛失败，恢复按钮可点
     */
    fun onWorshipFailed() {
        binding.tvSubmit.alpha = 1f
    }
}

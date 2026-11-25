package com.fingertip.uilib.dialog

import android.content.Context
import android.content.DialogInterface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fingertip.baselib.bean.LampDetailData
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.baselib.R as BaseR
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogLampActionBinding

/**
 * 佛灯操作弹窗。
 *
 * 灯油是消耗品，不属于装扮商店的 tab；用户点拜佛页的佛灯后在这里选
 * 点灯 / 续燃 / 切换灯具。购买动作交给页面统一走 dress/buyByCoin，
 * 弹窗只收用户选择，避免同一个购买链路在两处分叉。
 */
class LampActionDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    var onBuy: ((oil: ShopDressConfig, relight: Boolean) -> Unit)? = null
    var onSwitchDress: (() -> Unit)? = null

    private lateinit var binding: DialogLampActionBinding
    private var oilList: List<ShopDressConfig> = emptyList()
    private var selectedOilIndex = 0
    private var lamp: LampDetailData? = null
    private var relight = false

    override fun getLayoutId(): Int = R.layout.dialog_lamp_action

    override fun onViewCreate(view: View) {
        binding = DialogLampActionBinding.bind(view)
        binding.ivClose.setOnClickListener { dismiss() }
        binding.tvSwitchDress.setOnClickListener {
            //先存回调再关：onDismiss 会清空回调，dismiss 同步触发时直接 invoke 会变空
            val callback = onSwitchDress
            dismiss()
            callback?.invoke()
        }
        binding.tvModeLight.setOnClickListener {
            if (!lamp.isNullOrEmptyStatus()) selectMode(false)
        }
        binding.tvModeRelight.setOnClickListener {
            if (lamp?.isNone() == false) selectMode(true)
        }
        binding.tvSubmit.setOnClickListener {
            val oil = oilList.getOrNull(selectedOilIndex) ?: return@setOnClickListener
            onBuy?.invoke(oil, relight)
        }
        renderModes()
        renderBalance()
    }

    /** 灯油列表还没回来时先展示加载态，不让用户对着空弹窗猜 */
    fun showLoading() {
        ensureViewCreated()
        binding.tvStatus.setText(R.string.lamp_action_oil_loading)
        binding.llOils.removeAllViews()
        binding.tvBalance.text = ""
        setSubmitEnabled(false)
    }

    fun showLoadFailed() {
        ensureViewCreated()
        binding.tvStatus.setText(R.string.lamp_action_oil_fail)
        binding.llOils.removeAllViews()
        setSubmitEnabled(false)
    }

    /** 拿到灯油和灯状态后填充；lamp 为 null 按"无灯"处理 */
    fun render(oils: List<ShopDressConfig>, lampData: LampDetailData?) {
        ensureViewCreated()
        oilList = oils.filter { it.isOil() }
        lamp = lampData
        selectedOilIndex = 0
        renderStatus()
        renderModes()
        renderOils()
        renderBalance()
    }

    private fun LampDetailData?.isNullOrEmptyStatus(): Boolean = this == null || isNone()

    private fun renderStatus() {
        val data = lamp
        when {
            data == null || data.isNone() -> binding.tvStatus.setText(R.string.lamp_action_status_none)
            data.isDying() -> binding.tvStatus.text = context.getString(
                R.string.lamp_action_status_dying, data.remainHours
            )
            else -> binding.tvStatus.text = context.getString(
                R.string.lamp_action_status_burning, data.remainHours
            )
        }
    }

    private fun renderModes() {
        val canRelight = lamp?.isNone() == false
        //无灯只能点灯；已有灯只能续燃，避免"点了点灯结果服务端顺延"的误解
        relight = canRelight
        applyModeStyle(binding.tvModeLight, !canRelight, true)
        applyModeStyle(binding.tvModeRelight, canRelight, canRelight)
        binding.tvSubmit.setText(if (relight) R.string.lamp_action_relight else R.string.lamp_action_light)
    }

    private fun selectMode(isRelight: Boolean) {
        relight = isRelight
        applyModeStyle(binding.tvModeLight, !isRelight, true)
        applyModeStyle(binding.tvModeRelight, isRelight, true)
        binding.tvSubmit.setText(if (isRelight) R.string.lamp_action_relight else R.string.lamp_action_light)
    }

    private fun applyModeStyle(tv: TextView, selected: Boolean, enabled: Boolean) {
        tv.isEnabled = enabled
        tv.alpha = if (enabled) 1f else 0.45f
        tv.setBackgroundResource(
            if (selected) R.drawable.bg_worship_price_selected else R.drawable.bg_worship_price_normal
        )
        tv.setTextColor(
            TopApplication.instance.getColor(
                if (selected) BaseR.color.c_gold else BaseR.color.c_text_second
            )
        )
    }

    private fun renderOils() {
        val container = binding.llOils
        container.removeAllViews()
        if (oilList.isEmpty()) {
            val empty = TextView(context)
            empty.text = context.getString(R.string.lamp_action_oil_empty)
            empty.textSize = 13f
            empty.gravity = Gravity.CENTER
            empty.setPadding(0, 24, 0, 24)
            empty.setTextColor(TopApplication.instance.getColor(BaseR.color.c_text_second))
            container.addView(empty)
            setSubmitEnabled(false)
            return
        }
        oilList.forEachIndexed { index, item ->
            val tv = TextView(context)
            val name = item.dressName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.lamp_dress_oil)
            tv.text = "$name · ${item.durationText()}\n${item.priceText()}"
            tv.textSize = 13f
            tv.gravity = Gravity.CENTER
            tv.setPadding(0, 22, 0, 22)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 8
            container.addView(tv, lp)
            setOilStyle(tv, index == selectedOilIndex)
            tv.setOnClickListener {
                selectedOilIndex = index
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i) as? TextView ?: continue
                    setOilStyle(child, i == selectedOilIndex)
                }
            }
        }
        setSubmitEnabled(true)
    }

    private fun setOilStyle(tv: TextView, selected: Boolean) {
        tv.setBackgroundResource(
            if (selected) R.drawable.bg_worship_price_selected else R.drawable.bg_worship_price_normal
        )
        tv.setTextColor(
            TopApplication.instance.getColor(
                if (selected) BaseR.color.c_gold else BaseR.color.c_text_second
            )
        )
    }

    private fun renderBalance() {
        binding.tvBalance.text = context.getString(
            R.string.lamp_action_balance, GlobalConfig.userData?.coin ?: 0
        )
    }

    private fun setSubmitEnabled(enabled: Boolean) {
        binding.tvSubmit.isEnabled = enabled
        binding.tvSubmit.alpha = if (enabled) 1f else 0.5f
    }

    override fun onDismiss(dialog: DialogInterface?) {
        onBuy = null
        onSwitchDress = null
    }
}

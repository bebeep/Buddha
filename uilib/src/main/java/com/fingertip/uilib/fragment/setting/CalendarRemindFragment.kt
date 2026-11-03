package com.fingertip.uilib.fragment.setting

import android.view.View
import com.fingertip.baselib.bean.BuddhaFestival
import com.fingertip.baselib.bean.CalendarRemindData
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentCalendarRemindBinding
import com.fingertip.uilib.databinding.ItemRemindCustomBinding
import com.fingertip.uilib.databinding.ItemRemindPreviewBinding
import com.fingertip.uilib.dialog.ActionSheetDialog
import com.fingertip.uilib.dialog.ConfirmDialog
import com.fingertip.uilib.dialog.RemindEditDialog
import com.fingertip.uilib.push.PushManager
import com.fingertip.uilib.viewmodel.RemindVM
import java.util.Locale

/**
 * 佛历订阅提醒（我的 → 佛历订阅提醒）
 *
 * 这里只负责「配置」，不做任何本地调度：规则存服务端，由 CalendarRemindTask
 * 按每个账号上报的时区在本地整点推送。这么设计是因为本地 AlarmManager 在
 * Android 12+ 要精确闹钟权限、国产 ROM 还会清后台，跨时区也没法自适应。
 *
 * 三类规则：
 * - 佛历节日：可设提前 0~3 天
 * - 斋日：农历初一、十五当天
 * - 每日自定义：用户自己写一句话 + 每天几点
 *
 * 开关不做本地乐观更新：提醒配错了会真的打扰用户，必须等服务端确认再改界面。
 */
class CalendarRemindFragment : TopPmFragment<RemindVM>() {

    companion object {
        /** 预告列表往后看多少天 */
        private const val PREVIEW_DAYS = 90

        /** 预告最多列几条，再多列表就变成"节日大全"了 */
        private const val PREVIEW_LIMIT = 8
    }

    override fun layoutId() = R.layout.fragment_calendar_remind

    override fun initVM(): RemindVM = provideVM()

    private val binding get() = mBinding as FragmentCalendarRemindBinding

    /** 程序化设置开关时置 true，避免触发监听又把状态写回服务端 */
    private var syncing = false

    private var festivalRule: CalendarRemindData? = null
    private var zhaiRule: CalendarRemindData? = null

    /** 正在等待删除结果的那条，成功后从列表摘掉 */
    private var pendingDelete: CalendarRemindData? = null

    override fun initShiTu() {
        binding.switchFestival.setOnCheckedChangeListener { _, checked ->
            if (syncing) return@setOnCheckedChangeListener
            festivalRule?.let { mViewModel.switchRemind(it, checked) }
        }
        binding.switchZhai.setOnCheckedChangeListener { _, checked ->
            if (syncing) return@setOnCheckedChangeListener
            zhaiRule?.let { mViewModel.switchRemind(it, checked) }
        }

        mViewModel.loadRemindList()
        mViewModel.loadFestivals(PREVIEW_DAYS)
    }

    /**
     * 从系统设置页回来时权限可能已经变了，每次回到前台都刷一次引导条
     */
    override fun onResume() {
        super.onResume()
        refreshPushTip()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.remindListResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                renderRules(result.data ?: emptyList())
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.remind_save_fail))
            }
        }

        mViewModel.saveResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.remind_save_done))
                // 重新拉一次：服务端会补齐 id / 时刻文案，本地拼的副本不可靠
                mViewModel.loadRemindList()
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.remind_save_fail))
                // 失败要把开关拨回真实状态，否则界面在骗用户
                mViewModel.loadRemindList()
            }
        }

        mViewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val target = pendingDelete
            pendingDelete = null
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.remind_delete_done))
                mViewModel.loadRemindList()
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.remind_save_fail))
                if (target != null) {
                    // 删除失败就把这条重新画回来（renderRules 后没有本地乐观删除，不需要额外处理）
                    mViewModel.loadRemindList()
                }
            }
        }

        mViewModel.festivalResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            renderFestivalPreview(if (result.success) result.data ?: emptyList() else emptyList())
        }
    }

    //region 渲染

    private fun renderRules(list: List<CalendarRemindData>) {
        festivalRule = list.firstOrNull { it.remindType == CalendarRemindData.TYPE_FESTIVAL }
        zhaiRule = list.firstOrNull { it.remindType == CalendarRemindData.TYPE_ZHAI }
        val customs = list.filter { it.remindType == CalendarRemindData.TYPE_CUSTOM }

        syncing = true
        binding.switchFestival.isChecked = festivalRule?.enabled() == true
        binding.switchZhai.isChecked = zhaiRule?.enabled() == true
        syncing = false

        binding.tvFestivalTime.text = festivalRule?.timeLabel() ?: "08:00"
        binding.tvFestivalOffset.text = festivalRule?.offsetLabel() ?: getString(R.string.remind_offset_today)
        binding.tvZhaiTime.text = zhaiRule?.timeLabel() ?: "08:00"

        renderCustom(customs)
    }

    private fun renderCustom(customs: List<CalendarRemindData>) {
        binding.llCustomContainer.removeAllViews()
        binding.tvCustomEmpty.visibility = if (customs.isEmpty()) View.VISIBLE else View.GONE

        customs.forEach { rule ->
            val item = ItemRemindCustomBinding.inflate(layoutInflater, binding.llCustomContainer, false)
            item.tvTitle.text = rule.festivalKey.orEmpty()
            item.tvTime.text = "每天 ${rule.timeLabel()}"
            item.tvSwitch.text = getString(
                if (rule.enabled()) R.string.remind_enabled else R.string.remind_disabled
            )
            item.tvSwitch.setTextColor(
                if (rule.enabled()) 0xFFC4A265.toInt() else 0xFF8C8069.toInt()
            )
            item.tvSwitch.setOnClickListener {
                mViewModel.switchRemind(rule, !rule.enabled())
            }
            item.tvDelete.setOnClickListener { confirmDelete(rule) }
            binding.llCustomContainer.addView(item.root)
        }
    }

    private fun renderFestivalPreview(list: List<BuddhaFestival>) {
        binding.llPreviewContainer.removeAllViews()
        if (list.isEmpty()) {
            binding.tvPreviewEmpty.visibility = View.VISIBLE
            binding.llPreviewContainer.visibility = View.GONE
            return
        }
        binding.tvPreviewEmpty.visibility = View.GONE
        binding.llPreviewContainer.visibility = View.VISIBLE

        list.take(PREVIEW_LIMIT).forEach { festival ->
            val item = ItemRemindPreviewBinding.inflate(layoutInflater, binding.llPreviewContainer, false)
            item.tvDate.text = monthDay(festival.solarDate)
            item.tvName.text = festival.name.orEmpty()
            item.tvLunar.text = "农历${festival.lunarDate.orEmpty()}"
            item.tvCountdown.text = when {
                festival.today || festival.daysFromToday == 0 -> getString(R.string.remind_preview_today)
                else -> getString(R.string.remind_preview_days, festival.daysFromToday)
            }
            binding.llPreviewContainer.addView(item.root)
        }
    }

    /** 「2026-12-08」→「12-08」 */
    private fun monthDay(date: String?): String {
        val parts = date?.split("-") ?: return ""
        if (parts.size < 3) return date.orEmpty()
        return String.format(Locale.US, "%s-%s", parts[1], parts[2])
    }

    private fun refreshPushTip() {
        val granted = PushManager.hasPermission()
        binding.llPushTip.visibility = if (granted) View.GONE else View.VISIBLE
    }

    //endregion

    //region 交互

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack,
        binding.llFestivalTime,
        binding.llFestivalOffset,
        binding.llZhaiTime,
        binding.tvAddCustom,
        binding.tvPushTipAction
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()

            binding.llFestivalTime -> editTime(festivalRule)
            binding.llZhaiTime -> editTime(zhaiRule)

            binding.llFestivalOffset -> chooseOffset()

            binding.tvAddCustom -> addCustom()

            binding.tvPushTipAction -> PushManager.requestPermission {
                refreshPushTip()
            }
        }
    }

    /**
     * 改时刻。规则还没加载到就忽略点击，避免拿 null 去拼请求
     */
    private fun editTime(rule: CalendarRemindData?) {
        rule ?: return
        RemindEditDialog(
            context = requireContext(),
            title = getString(R.string.remind_dialog_time_title),
            showTextInput = false,
            initialHour = rule.hour()
        ) { _, hour ->
            val copy = copyWith(rule, hour = hour)
            mViewModel.saveRemind(copy)
        }.show()
    }

    /**
     * 提前天数只做 4 档：再早用户也记不住，列表还会变长
     */
    private fun chooseOffset() {
        val rule = festivalRule ?: return
        val items = listOf(0, -1, -2, -3).map { offset ->
            ActionSheetDialog.Item(
                text = if (offset == 0) getString(R.string.remind_offset_today)
                else getString(R.string.remind_offset_days, -offset),
                onClick = {
                    if (rule.remindOffset != offset) {
                        mViewModel.saveRemind(copyWith(rule, offset = offset))
                    }
                }
            )
        }
        ActionSheetDialog(
            requireContext(),
            title = getString(R.string.remind_offset_dialog_title),
            items = items
        ).show()
    }

    private fun addCustom() {
        val current = mViewModel.remindListResult.value?.data.orEmpty()
        val count = current.count { it.remindType == CalendarRemindData.TYPE_CUSTOM }
        if (count >= CalendarRemindData.MAX_CUSTOM) {
            ToastUtil.showMessage(getString(R.string.remind_custom_limit, CalendarRemindData.MAX_CUSTOM))
            return
        }
        RemindEditDialog(
            context = requireContext(),
            title = getString(R.string.remind_custom_name),
            showTextInput = true,
            initialHour = CalendarRemindData().hour()
        ) { text, hour ->
            val rule = CalendarRemindData()
            rule.remindType = CalendarRemindData.TYPE_CUSTOM
            rule.festivalKey = text
            rule.remindOffset = 0
            rule.isEnabled = 1
            mViewModel.saveRemind(copyWith(rule, hour = hour))
        }.show()
    }

    private fun confirmDelete(rule: CalendarRemindData) {
        ConfirmDialog(
            requireContext(),
            title = getString(R.string.remind_delete_title),
            message = getString(R.string.remind_delete_msg, rule.festivalKey.orEmpty()),
            confirmText = getString(R.string.remind_delete),
            cancelText = getString(R.string.remind_dialog_cancel)
        ) {
            pendingDelete = rule
            mViewModel.deleteRemind(rule.id)
        }.show()
    }

    //endregion

    /**
     * 用原规则复制一份改几个字段。
     * 不直接改原对象：改完请求若失败，界面上的旧值已经被污染了
     */
    private fun copyWith(
        rule: CalendarRemindData,
        hour: Int = rule.hour(),
        offset: Int = rule.remindOffset
    ): CalendarRemindData {
        val copy = CalendarRemindData()
        copy.id = rule.id
        copy.remindType = rule.remindType
        copy.festivalKey = rule.festivalKey
        copy.remindOffset = if (rule.remindType == CalendarRemindData.TYPE_FESTIVAL) offset else 0
        copy.remindTime = rule.remindTime
        copy.remindTimeText = String.format(Locale.US, "%02d:00", hour)
        copy.isEnabled = rule.isEnabled
        return copy
    }
}

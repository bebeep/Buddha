package com.fingertip.uilib.fragment.practice

import android.content.Context
import android.view.View
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentMeditationBinding
import com.fingertip.uilib.viewmodel.MeditationVM
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R as BaseR

/**
 * 念佛计数
 *
 * - 点念珠一声 +1，今日计数按「日期 + 佛号」存本机 SharedPreferences，
 *   换会话回来还在（服务端没有"今日单佛号计数"的查询接口，硬要加一个只为显示不值当；
 *   功德币结算在服务端，本地这份只是让用户看见今天的数）。
 * - 上报节流：满 108 声报一次、切佛号报一次、离开页面报一次。
 *   点一下就发一次请求既费流量又会被限流，而且服务端本来就是按 108 声的档位结算的。
 * - 累计数从服务端拉（全佛号合计），下拉/重进都会刷新。
 * - 未登录只本地计数，不上报，页脚提示登录后才有功德币。
 */
class MeditationFragment : TopPmFragment<MeditationVM>() {

    companion object {
        private const val SP_NAME = "meditation_count"
        //满这个数就上报一次，与服务端"每满 108 声 1 功德币"的结算档位对齐
        private const val FLUSH_UNIT = 108
    }

    override fun layoutId() = R.layout.fragment_meditation

    override fun initVM(): MeditationVM = provideVM()

    private val binding get() = mBinding as FragmentMeditationBinding

    //佛号名固定不跟文案走：服务端按字符串记账，改文案就改成了新佛号，统计会断
    private val holyNames = arrayOf("阿弥陀佛", "观音圣号", "六字大明咒")

    //当前选中的佛号下标
    private var selectedChip = 0

    //当前佛号的今日计数
    private var todayCount = 0

    //服务端累计总声数
    private var totalCount = 0

    //还没上报的声数
    private var pending = 0

    //本段计时起点（用于上报 addSeconds）
    private var segStart = 0L

    private val sp by lazy {
        requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    override fun initShiTu() {
        todayCount = sp.getInt(keyOf(selectedChip), 0)
        segStart = System.currentTimeMillis()
        renderChips()
        renderCounts()
        if (GlobalConfig.session.isNotBlank()) {
            mViewModel.loadTotal()
        }
    }

    override fun initObserver() {
        super.initObserver()
        mViewModel.totalResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val total = result.data
            if (result.success && total != null) {
                totalCount = total
                renderCounts()
            }
        }
        //上报成功不提示；失败也不打扰（下一段会带着余数一起补报）
        mViewModel.reportResult.observe(viewLifecycleOwner) {
            //no-op
        }
    }

    /**
     * 佛号 chips：选中的用金描边 + 亮金字，其余用暗底 + 弱化字
     */
    private fun renderChips() {
        val chips = listOf(binding.tvChip1, binding.tvChip2, binding.tvChip3)
        chips.forEachIndexed { index, chip ->
            val selected = index == selectedChip
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_home_pill_outline else R.drawable.bg_home_pill_solid
            )
            chip.setTextColor(if (selected) TopApplication.instance.getColor(BaseR.color.c_gold_light) else TopApplication.instance.getColor(BaseR.color.c_text_second))
        }
        binding.tvBeadText.text = chips.getOrNull(selectedChip)?.text
    }

    private fun renderCounts() {
        binding.tvTodayCount.text = todayCount.toString()
        binding.tvTotalCount.text = totalCount.toString()
    }

    override fun getClickViews() = listOf(
        binding.ivBack,
        binding.flBead,
        binding.tvChip1,
        binding.tvChip2,
        binding.tvChip3
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> {
                flush()
                pop()
            }
            //念珠大按钮：一声佛号 +1
            binding.flBead -> onChant()
            binding.tvChip1 -> switchChip(0)
            binding.tvChip2 -> switchChip(1)
            binding.tvChip3 -> switchChip(2)
        }
    }

    private fun onChant() {
        todayCount++
        totalCount++
        pending++
        sp.edit().putInt(keyOf(selectedChip), todayCount).apply()
        renderCounts()
        if (pending >= FLUSH_UNIT) {
            flush()
        }
    }

    /**
     * 上报未发送的计数。切佛号/离开页面前必须调——
     * 否则余数留在本地，服务端永远少记一段，功德币也就少了。
     */
    private fun flush() {
        if (pending <= 0) return
        if (GlobalConfig.session.isBlank()) {
            //游客态不上报，累计数也就没有服务端口径，只保留本地今日数
            pending = 0
            segStart = System.currentTimeMillis()
            return
        }
        val seconds = ((System.currentTimeMillis() - segStart) / 1000).toInt().coerceAtLeast(0)
        mViewModel.report(holyNames[selectedChip], pending, seconds)
        pending = 0
        segStart = System.currentTimeMillis()
    }

    private fun switchChip(index: Int) {
        if (selectedChip == index) return
        //先把上一佛号的余数报掉，否则这段计数会记到新佛号名下
        flush()
        selectedChip = index
        todayCount = sp.getInt(keyOf(index), 0)
        renderChips()
        renderCounts()
    }

    /**
     * 今日计数的 key：日期 + 佛号，跨天自动从 0 开始，不需要额外清数据
     */
    private fun keyOf(index: Int): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return "c_${date}_$index"
    }

    override fun onPause() {
        super.onPause()
        flush()
    }
}

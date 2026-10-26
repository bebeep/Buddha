package com.fingertip.uilib.fragment.worshiping

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.BuddhaFestival
import com.fingertip.baselib.bean.CalendarSimpleRsp
import com.fingertip.baselib.bean.SignInData
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CalendarDayAdapter
import com.fingertip.uilib.databinding.FragmentSignInBinding
import com.fingertip.uilib.databinding.ItemSignFestivalBinding
import com.fingertip.uilib.dialog.BuddhaFestivalDialog
import com.fingertip.uilib.viewmodel.SignInCalendarVM
import java.util.Calendar

/**
 * 签到页，对应原型「签到」与接口 account/getMonthCalendar
 *
 * 服务端按 ISO 周序排格子（周一开头），上/下月补位日期一并返回并淡化显示；
 * 签到成功后重新拉月历刷新格子与按钮状态。
 *
 * 佛历节日（佛菩萨圣诞/成道/出家/涅槃、盂兰盆/卫塞节等）也随月历一起回来：
 * 格子里用节日短名替换农历日，页面上方给今日/下一个节日横幅，日历下方列本月节日，
 * 点条目看简介。数据全部来自一次月历请求，不额外发接口。
 */
class SignInFragment : TopPmFragment<SignInCalendarVM>() {

    companion object {
        private const val TAG = "SignInFragment"

        //ISO 周序：周一=1 … 周日=7，与服务端 getWeekDayValue 对齐
        private val WEEK_LABELS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

        //连签进度展示的档位数
        private const val DOT_COUNT = 7

        //服务端「今天已经签到过了」
        private const val CODE_TODAY_SIGNED = -24

        //佛菩萨圣诞/成道/出家/涅槃
        private const val FESTIVAL_COLOR_MAJOR = "#D9432E"

        //盂兰盆、卫塞节等佛教节日
        private const val FESTIVAL_COLOR_MINOR = "#C8860D"
    }

    override fun layoutId(): Int = R.layout.fragment_sign_in

    override fun initVM(): SignInCalendarVM = provideVM()

    private val binding get() = mBinding as FragmentSignInBinding

    private lateinit var calendarAdapter: CalendarDayAdapter

    //当前展示的年月
    private var showYear = 0
    private var showMonth = 0

    //最近一次月历数据，判断今天是否已签到用
    private var calendarRsp: CalendarSimpleRsp? = null

    //签到请求进行中，防连点
    private var signing = false

    override fun initShiTu() {
        initCalendar()
        initDots()
        initMonthSwitcher()
        loadCurrentMonth()
        mViewModel.loadSignInData()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.calendarResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                calendarRsp = result.data
                renderCalendar(result.data)
            } else {
                log(TAG, "月历获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "月历加载失败，请稍后再试")
            }
        }

        mViewModel.signInDataResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                renderContinuous(result.data)
            }
        }

        mViewModel.signInResult.observe(viewLifecycleOwner) { result ->
            signing = false
            if (result.success) {
                ToastUtil.showMessage("签到成功")
                //重新拉月历和签到信息，格子、按钮、连签进度一起刷
                mViewModel.loadCalendar(showYear, showMonth)
                mViewModel.loadSignInData()
            } else {
                if (result.errorCode == CODE_TODAY_SIGNED) {
                    calendarRsp?.hasSignIn = true
                }
                ToastUtil.showMessage(result.errorMsg ?: "签到失败，请稍后再试")
                renderSignButton()
            }
        }
    }

    /**
     * 日历格子：7 列网格，禁止自身滚动（外面有 ScrollView）
     */
    private fun initCalendar() {
        calendarAdapter = CalendarDayAdapter(requireContext())
        binding.rcCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rcCalendar.adapter = calendarAdapter
        binding.rcCalendar.isNestedScrollingEnabled = false

        //表头：周一…周日，与服务端网格列序一致
        val header = binding.llWeekHeader
        WEEK_LABELS.forEach { label ->
            val tv = TextView(requireContext())
            tv.text = label
            tv.setTextColor(Color.parseColor("#666666"))
            tv.textSize = 13f
            tv.gravity = android.view.Gravity.CENTER
            header.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    /**
     * 连签进度：7 个圆点，达到的天数填充
     */
    private fun initDots() {
        val dots = binding.llDots
        val size = (resources.displayMetrics.density * 22).toInt()
        repeat(DOT_COUNT) { index ->
            val tv = TextView(requireContext())
            tv.text = (index + 1).toString()
            tv.textSize = 10f
            tv.gravity = android.view.Gravity.CENTER
            val lp = LinearLayout.LayoutParams(size, size)
            if (index > 0) lp.marginStart = (resources.displayMetrics.density * 6).toInt()
            dots.addView(tv, lp)
        }
    }

    /**
     * 月份切换：接口限当前时间前后一年
     */
    private fun initMonthSwitcher() {
        binding.tvPreMonth.setOnClickListener {
            shiftMonth(-1)
        }
        binding.tvNextMonth.setOnClickListener {
            shiftMonth(1)
        }
    }

    private fun shiftMonth(delta: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(showYear, showMonth - 1 + delta, 1)
        loadMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }

    private fun loadCurrentMonth() {
        val now = Calendar.getInstance()
        loadMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }

    private fun loadMonth(year: Int, month: Int) {
        showYear = year
        showMonth = month
        binding.tvMonth.text = "${year}年${month}月"
        mViewModel.loadCalendar(year, month)
    }

    /**
     * 渲染月历：格子 + 佛历节日 + 今日详情卡 + 签到按钮
     */
    private fun renderCalendar(rsp: CalendarSimpleRsp?) {
        if (rsp == null) return
        calendarAdapter.setData(showYear, showMonth, rsp)
        renderFestivals(rsp)
        renderTodayInfo(rsp)
        renderSignButton()
    }

    /**
     * 佛历节日两块：顶部横幅（今天有节日显示今天，否则显示下一个节日倒计时）
     * 和日历下方「本月佛历节日」列表
     */
    private fun renderFestivals(rsp: CalendarSimpleRsp) {
        renderFestivalBanner(rsp)
        renderFestivalList(rsp.festivals)
    }

    private fun renderFestivalBanner(rsp: CalendarSimpleRsp) {
        val today = rsp.todayFestival
        val target = today ?: rsp.nextFestival
        if (target == null) {
            binding.llFestivalBanner.visibility = View.GONE
            return
        }
        val isToday = today != null
        binding.llFestivalBanner.visibility = View.VISIBLE
        binding.llFestivalBanner.setBackgroundResource(
            if (isToday) R.drawable.bg_sign_festival_today else R.drawable.bg_sign_festival_next
        )
        binding.tvFestivalTag.text = if (isToday) "今日" else "下一个"
        binding.tvFestivalTag.setBackgroundResource(
            if (isToday) R.drawable.bg_festival_tag_major else R.drawable.bg_festival_tag_minor
        )

        binding.tvFestivalName.text = target.name.orEmpty()
        binding.tvFestivalName.setTextColor(
            Color.parseColor(if (isToday) FESTIVAL_COLOR_MAJOR else FESTIVAL_COLOR_MINOR)
        )
        binding.tvFestivalCountdown.text = if (isToday) "" else "还有${target.daysFromToday}天"

        binding.tvFestivalMeta.text = listOfNotNull(
            target.lunarDate?.takeIf { it.isNotBlank() }?.let { "农历$it" },
            target.dateLabel().takeIf { it.isNotBlank() }
        ).joinToString("  ·  ")

        val desc = target.description?.takeIf { it.isNotBlank() }
        binding.tvFestivalDesc.visibility = if (desc == null) View.GONE else View.VISIBLE
        binding.tvFestivalDesc.text = desc.orEmpty()
    }

    /**
     * 本月佛历节日列表，条目点开看简介
     */
    private fun renderFestivalList(festivals: List<BuddhaFestival>?) {
        val items = festivals.orEmpty()
        binding.llFestivalCard.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        binding.tvFestivalCount.text = "共${items.size}个"
        binding.llFestivalItems.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())
        items.forEach { festival ->
            val itemBinding = ItemSignFestivalBinding.inflate(inflater, binding.llFestivalItems, false)
            val color = Color.parseColor(
                if (festival.level == BuddhaFestival.LEVEL_FESTIVAL) FESTIVAL_COLOR_MINOR else FESTIVAL_COLOR_MAJOR
            )
            itemBinding.vAccent.setBackgroundColor(color)
            itemBinding.tvName.text = festival.name.orEmpty()
            itemBinding.tvMeta.text = listOfNotNull(
                festival.lunarDate?.takeIf { it.isNotBlank() }?.let { "农历$it" },
                festival.dateLabel().takeIf { it.isNotBlank() }
            ).joinToString("  ·  ")
            itemBinding.tvCountdown.text = festival.countdownLabel()
            itemBinding.tvCountdown.setTextColor(
                when {
                    festival.daysFromToday == 0 -> color
                    festival.daysFromToday > 0 -> Color.parseColor(FESTIVAL_COLOR_MINOR)
                    else -> Color.parseColor("#C0C0C0")
                }
            )
            itemBinding.root.setOnClickListener {
                BuddhaFestivalDialog(requireContext(), festival).show()
            }
            binding.llFestivalItems.addView(itemBinding.root)
        }
    }

    /**
     * 今日详情卡：佛历、农历、干支生肖、宜忌
     */
    private fun renderTodayInfo(rsp: CalendarSimpleRsp) {
        val info = rsp.calendarInfo ?: return
        binding.tvBuddhistYear.text = "佛历${info.buddhistYear}年"
        binding.tvLunar.text = info.lunarDate.orEmpty()

        //干支/生肖/五行，服务端可能为空，能拼多少拼多少
        val parts = listOfNotNull(
            info.zodiac?.takeIf { it.isNotBlank() }?.let { "${it}年" },
            info.ganZhi?.takeIf { it.isNotBlank() }?.let { "$it 日" },
            info.wuxing?.takeIf { it.isNotBlank() }?.let { "五行 $it" }
        )
        binding.tvGanzhi.text = parts.joinToString("  ·  ")

        val suitable = info.suitable?.takeIf { it.isNotBlank() }
        val avoid = info.avoid?.takeIf { it.isNotBlank() }
        binding.llSuitable.visibility = if (suitable == null) View.GONE else View.VISIBLE
        binding.llAvoid.visibility = if (avoid == null) View.GONE else View.VISIBLE
        binding.tvSuitable.text = suitable.orEmpty()
        binding.tvAvoid.text = avoid.orEmpty()
    }

    /**
     * 连签进度：圆点按已连签天数填充
     */
    private fun renderContinuous(signInData: SignInData?) {
        if (signInData == null) {
            binding.llContinuous.visibility = View.GONE
            return
        }
        binding.llContinuous.visibility = View.VISIBLE
        binding.tvContinuousDays.text = signInData.continuousDays.toString()

        val reached = signInData.continuousDays
        val dots = binding.llDots
        for (index in 0 until dots.childCount) {
            val tv = dots.getChildAt(index) as? TextView ?: continue
            val isReached = index < reached.coerceAtMost(DOT_COUNT)
            tv.setBackgroundResource(
                if (isReached) R.drawable.bg_sign_dot_reached else R.drawable.bg_sign_dot_normal
            )
            tv.setTextColor(if (isReached) Color.WHITE else Color.parseColor("#C89A5B"))
        }
    }

    /**
     * 底部按钮：已签到/签到中置灰，防止重复提交
     */
    private fun renderSignButton() {
        val hasSignIn = calendarRsp?.hasSignIn ?: false
        when {
            signing -> {
                binding.tvSignBtn.text = "签到中…"
                binding.tvSignBtn.alpha = 0.6f
            }
            hasSignIn -> {
                binding.tvSignBtn.text = "今日已签到"
                binding.tvSignBtn.alpha = 0.6f
            }
            else -> {
                binding.tvSignBtn.text = "立即签到"
                binding.tvSignBtn.alpha = 1f
            }
        }
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack,
        binding.tvSignBtn
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvSignBtn -> {
                if (signing || calendarRsp?.hasSignIn == true) return
                signing = true
                renderSignButton()
                mViewModel.signIn()
            }
        }
    }
}

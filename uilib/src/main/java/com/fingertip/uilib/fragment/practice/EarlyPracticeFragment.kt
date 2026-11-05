package com.fingertip.uilib.fragment.practice

import android.content.Context
import android.os.CountDownTimer
import android.view.View
import com.blankj.utilcode.util.GsonUtils
import com.fingertip.baselib.bean.PracticeStatisticData
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentEarlyPracticeBinding
import com.fingertip.uilib.viewmodel.EarlyPracticeVM
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R as BaseR

/**
 * 早课五步流程：礼佛 → 诵经 → 称念 → 发愿 → 回向
 *
 * 设计要点：
 * 1. 一条固定路径走完算一次修持，中途退出不留记录（服务端一人一天只允许一条）。
 * 2. 每一步都有"完成条件"，条件没达成就卡在当前步——这是仪式感，也是防刷：
 *    礼佛要真点满 3 次，诵经要真读满 60 秒，称念要真点满 21 声。
 * 3. 发愿只存本机 SharedPreferences，不上传：一是零审核成本，二是愿文属于私密内容，
 *    没有服务端留存就没有泄露风险。回向卡上只显示有没有写，不做回显以外的用途。
 * 4. 时区交给服务端判定早课/晚课：客户端只上报 IANA 时区 id（Asia/Singapore 这种），
 *    海外用户跨时区不会把"晚上"算成"早上"。
 * 5. 结果页的功德币与连续天数全部来自服务端（提交后重拉统计），不在客户端自增，
 *    否则改本地数据就能刷里程碑奖励。
 */
class EarlyPracticeFragment : TopPmFragment<EarlyPracticeVM>() {

    companion object {
        //礼佛次数目标
        private const val WORSHIP_TARGET = 3
        //诵经最少秒数，需与服务端时长下限对齐
        private const val CHANT_SECONDS = 60
        //称念佛号目标
        private const val NIANFO_TARGET = 21
        //单次早课固定功德币，仅用于结果页文案（真实发放以服务端为准）
        private const val PRACTICE_COIN = 20
        //愿文本地存储
        private const val SP_NAME = "early_practice"
        private const val SP_KEY_VOW = "last_vow"
    }

    override fun layoutId() = R.layout.fragment_early_practice

    override fun initVM(): EarlyPracticeVM = provideVM()

    private val binding get() = mBinding as FragmentEarlyPracticeBinding

    private val steps by lazy {
        listOf(binding.llStep1, binding.llStep2, binding.llStep3, binding.llStep4, binding.llStep5)
    }

    private val dots by lazy {
        listOf(binding.tvDot1, binding.tvDot2, binding.tvDot3, binding.tvDot4, binding.tvDot5)
    }

    //当前步骤 1..5
    private var step = 1

    private var worshipCount = 0
    private var chantElapsed = 0
    private var nianfoCount = 0

    //进页时间，用于上报总时长
    private var enterTime = 0L

    private var chantTimer: CountDownTimer? = null

    //提交成功后等统计回来再渲染结果页，避免连续天数显示成旧值
    private var justSubmitted = false

    //今天是否已经做过早课
    private var todayDone = false

    private val sp by lazy {
        requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack,
        binding.tvAction,
        binding.tvWorshipBtn,
        binding.tvChantStart,
        binding.tvNianfoBtn
    )

    override fun initShiTu() {
        enterTime = System.currentTimeMillis()
        //上次写过的愿文先回填，方便用户延续（本机存储，不上传）
        binding.etVow.setText(sp.getString(SP_KEY_VOW, ""))
        renderDots()
        renderStep()
        //未登录不必拉统计，页面照常可走完，只是提交时会提示；
        //带 loading 的请求等入栈动画放完再发，避免转场被 HUD + 请求卡顿
        if (GlobalConfig.session.isNotBlank()) {
            postOnEnterAnimationEnd {
                mViewModel.loadStatistic()
                //第二步诵经的经文：按天随机取，失败保持写死的心经兜底
                mViewModel.loadDailySutra()
            }
        }
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.sutraResult.observe(viewLifecycleOwner) { sutra ->
            if (!isAdded) return@observe
            binding.tvChantSource.text =
                getString(R.string.ep_chant_source, sutra.bookTitle, sutra.chapterTitle)
            binding.tvChantSource.visibility = View.VISIBLE
            binding.tvChantText.text = sutra.content
        }

        mViewModel.statisticResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val data = result.data
            if (!result.success || data == null) {
                return@observe
            }
            if (justSubmitted) {
                justSubmitted = false
                showDoneResult(data.currentStreak)
            } else if (data.lastPracticeDate == todayStr()) {
                //今天已经做过：直接进结果态，不让用户白走五步最后被服务端拒绝
                todayDone = true
                showAlreadyResult()
            }
        }

        mViewModel.submitResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (!result.success) {
                //TODAY_HAS_SIGN_IN 是服务端"今日已修持"的返回码，这种情况按已完成展示
                if (result.errorMsg?.contains("签到") == true || result.errorMsg?.contains("今天") == true) {
                    todayDone = true
                    showAlreadyResult()
                } else {
                    setAction(true, getString(R.string.ep_finish))
                    ToastUtil.showMessage(result.errorMsg ?: getString(R.string.ep_submit_fail))
                }
                return@observe
            }
            //提交成功：重拉统计拿服务端算的连续天数
            justSubmitted = true
            mViewModel.loadStatistic()
        }
    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> goBack()
            binding.tvAction -> onActionClick()
            binding.tvWorshipBtn -> onWorship()
            binding.tvChantStart -> startChant()
            binding.tvNianfoBtn -> onNianfo()
        }
    }

    // ==================== 步骤控制 ====================

    /**
     * 返回：中间的步骤退回上一步，第一步直接退出页面。
     * 不做"退出会丢失进度"的二次确认——本来就没有服务端进度，退出去也不会少什么。
     */
    private fun goBack() {
        if (todayDone) {
            pop()
            return
        }
        if (step > 1) {
            step--
            renderStep()
            renderDots()
        } else {
            pop()
        }
    }

    private fun onActionClick() {
        if (todayDone) {
            pop()
            return
        }
        if (!currentStepDone()) {
            ToastUtil.showMessage(getString(R.string.ep_need_current))
            return
        }
        when (step) {
            5 -> submit()
            else -> {
                step++
                renderStep()
                renderDots()
            }
        }
    }

    /**
     * 当前步是否达到完成条件
     */
    private fun currentStepDone(): Boolean = when (step) {
        1 -> worshipCount >= WORSHIP_TARGET
        2 -> chantElapsed >= CHANT_SECONDS
        3 -> nianfoCount >= NIANFO_TARGET
        4 -> true //愿文可以不写，写与不写都是个人事，不做强制
        else -> true
    }

    private fun renderStep() {
        steps.forEachIndexed { index, view ->
            view.visibility = if (index + 1 == step) View.VISIBLE else View.GONE
        }
        binding.llResult.visibility = View.GONE
        binding.tvStep.text = getString(R.string.ep_step_of, step)

        //每步进入时刷新自己的计数文案（从后步退回来也要正确）
        when (step) {
            1 -> binding.tvWorshipCount.text = getString(R.string.ep_worship_count, worshipCount)
            2 -> binding.tvChantTimer.text =
                if (chantElapsed >= CHANT_SECONDS) getString(R.string.ep_chant_done)
                else getString(R.string.ep_chant_counting, CHANT_SECONDS - chantElapsed)
            3 -> binding.tvNianfoCount.text = getString(R.string.ep_nianfo_count, nianfoCount)
            5 -> renderReview()
        }

        //主按钮在前四步始终可点：条件没达成就点了只提示，比灰着不给点更好懂；
        //第五步只在提交那一瞬间禁用，防重复提交
        setAction(true, if (step == 5) getString(R.string.ep_finish) else getString(R.string.ep_next))
    }

    /**
     * 主按钮态：金色实底 = 可提交/可继续；暗底 = 条件未达
     */
    private fun setAction(enabled: Boolean, text: String) {
        binding.tvAction.text = text
        binding.tvAction.setBackgroundResource(
            if (enabled) R.drawable.bg_ep_primary else R.drawable.bg_ep_primary_off
        )
        binding.tvAction.setTextColor(
            if (enabled) TopApplication.instance.getColor(BaseR.color.c_text_on_gold) else TopApplication.instance.getColor(BaseR.color.c_text_second)
        )
    }

    private fun renderDots() {
        dots.forEachIndexed { index, dot ->
            val no = index + 1
            when {
                no < step -> {
                    dot.setBackgroundResource(R.drawable.bg_home_dot_gold)
                    dot.text = "✓"
                    dot.setTextColor(TopApplication.instance.getColor(BaseR.color.c_text_on_gold))
                }
                no == step -> {
                    dot.setBackgroundResource(R.drawable.bg_home_circle)
                    dot.text = no.toString()
                    dot.setTextColor(TopApplication.instance.getColor(BaseR.color.c_gold_light))
                }
                else -> {
                    dot.setBackgroundResource(R.drawable.bg_home_circle)
                    dot.text = no.toString()
                    dot.setTextColor(TopApplication.instance.getColor(BaseR.color.c_text_hint2))
                }
            }
        }
    }

    // ==================== 各步行为 ====================

    /**
     * 礼佛：点一次算一拜，满 3 拜不再累加
     */
    private fun onWorship() {
        if (worshipCount >= WORSHIP_TARGET) return
        worshipCount++
        binding.tvWorshipCount.text = getString(R.string.ep_worship_count, worshipCount)
        if (worshipCount >= WORSHIP_TARGET) {
            setAction(true, getString(R.string.ep_next))
        }
    }

    /**
     * 诵经计时：倒计时 60 秒，读完才能进下一步。
     * 用 CountDownTimer 而不是 Handler 累加，是因为用户可能切后台，
     * 只要 Activity 还在，timer 能继续走；页面销毁时 onDestroyView 里会 cancel。
     */
    private fun startChant() {
        if (chantElapsed >= CHANT_SECONDS) return
        binding.tvChantStart.isEnabled = false
        binding.tvChantStart.alpha = 0.5f
        chantTimer?.cancel()
        chantTimer = object : CountDownTimer((CHANT_SECONDS * 1000L), 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isAdded) return
                val remain = (millisUntilFinished / 1000L).toInt()
                chantElapsed = CHANT_SECONDS - remain
                binding.tvChantTimer.text = getString(R.string.ep_chant_counting, remain)
            }

            override fun onFinish() {
                if (!isAdded) return
                chantElapsed = CHANT_SECONDS
                binding.tvChantTimer.text = getString(R.string.ep_chant_done)
                binding.tvChantStart.text = getString(R.string.ep_chant_done)
                binding.tvChantStart.alpha = 1f
                setAction(true, getString(R.string.ep_next))
            }
        }.start()
    }

    /**
     * 称念：点一次计一声，满 21 声不再累加
     */
    private fun onNianfo() {
        if (nianfoCount >= NIANFO_TARGET) return
        nianfoCount++
        binding.tvNianfoCount.text = getString(R.string.ep_nianfo_count, nianfoCount)
        if (nianfoCount >= NIANFO_TARGET) {
            setAction(true, getString(R.string.ep_finish))
        }
    }

    /**
     * 回向卡：把前四步的结果汇总给用户看一眼，再提交
     */
    private fun renderReview() {
        binding.tvReviewWorship.text = getString(R.string.ep_review_worship, worshipCount)
        binding.tvReviewChant.text = getString(R.string.ep_review_chant, chantElapsed)
        binding.tvReviewNianfo.text = getString(R.string.ep_review_nianfo, nianfoCount)
        val vow = binding.etVow.text?.toString()?.trim().orEmpty()
        binding.tvReviewVow.text =
            if (vow.isEmpty()) getString(R.string.ep_review_title) + "：" + getString(R.string.ep_vow_hint)
            else getString(R.string.ep_review_vow, vow)
    }

    // ==================== 提交 ====================

    private fun submit() {
        if (GlobalConfig.session.isBlank()) {
            ToastUtil.showMessage(getString(R.string.ep_login_first))
            return
        }
        val vow = binding.etVow.text?.toString()?.trim().orEmpty()
        //愿文只落本机，不进 stepData 以外的任何服务端存储
        sp.edit().putString(SP_KEY_VOW, vow).apply()

        val stepData = HashMap<String, Any>().apply {
            put("worship", worshipCount)
            put("chantSeconds", chantElapsed)
            put("nianfo", nianfoCount)
            //愿文只是"写没写"的标记，全文不上传
            put("vowWritten", vow.isNotEmpty())
        }
        //总时长至少按诵经时长兜底，避免用户手快导致服务端时长校验不过
        val totalSeconds = maxOf(((System.currentTimeMillis() - enterTime) / 1000).toInt(), CHANT_SECONDS)
        setAction(false, getString(R.string.ep_finish))
        mViewModel.submit(GsonUtils.toJson(stepData), totalSeconds)
    }

    // ==================== 结果态 ====================

    private fun showDoneResult(streak: Int) {
        todayDone = true
        steps.forEach { it.visibility = View.GONE }
        binding.llResult.visibility = View.VISIBLE
        binding.tvStep.text = ""
        binding.tvResultTitle.setText(R.string.ep_done_title)
        binding.tvResultDesc.text = getString(R.string.ep_done_desc, PRACTICE_COIN, streak)
        val next = PracticeStatisticData.nextMilestone(streak)
        binding.tvResultMilestone.text = if (next > 0) {
            getString(R.string.ep_done_milestone, next - streak)
        } else ""
        setAction(true, getString(R.string.ep_back))
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(R.drawable.bg_home_dot_gold)
            dot.text = "✓"
            dot.setTextColor(TopApplication.instance.getColor(BaseR.color.c_text_on_gold))
        }
    }

    private fun showAlreadyResult() {
        todayDone = true
        steps.forEach { it.visibility = View.GONE }
        binding.llResult.visibility = View.VISIBLE
        binding.tvStep.text = ""
        binding.tvResultTitle.setText(R.string.ep_already_title)
        binding.tvResultDesc.setText(R.string.ep_already_desc)
        binding.tvResultMilestone.text = ""
        setAction(true, getString(R.string.ep_back))
    }

    // ==================== 工具 ====================

    private fun todayStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onDestroyView() {
        chantTimer?.cancel()
        chantTimer = null
        super.onDestroyView()
    }
}

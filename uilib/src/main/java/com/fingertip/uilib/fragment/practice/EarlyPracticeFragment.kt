package com.fingertip.uilib.fragment.practice

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import com.blankj.utilcode.util.GsonUtils
import com.fingertip.baselib.bean.PracticeStatisticData
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.EarlyPracticePagerAdapter
import com.fingertip.uilib.databinding.FragmentEarlyPracticeBinding
import com.fingertip.uilib.databinding.ItemEpChantBinding
import com.fingertip.uilib.databinding.ItemEpNianfoBinding
import com.fingertip.uilib.databinding.ItemEpReviewBinding
import com.fingertip.uilib.databinding.ItemEpVowBinding
import com.fingertip.uilib.databinding.ItemEpWorshipBinding
import com.fingertip.uilib.view.earlypractice.EpTiltDetector
import com.fingertip.uilib.view.nianfo.NianfoRhythm
import com.fingertip.uilib.view.nianfo.NianfoWoodSound
import com.fingertip.uilib.viewmodel.EarlyPracticeVM
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.fingertip.baselib.R as BaseR

/**
 * 早课五步：礼佛 → 诵经 → 念佛 → 发愿 → 回向。
 *
 * 五步同页滑动，顶部展示进度与完成状态，底部主按钮按当前步条件置灰/点亮。
 * 步骤状态全在页面本地持有，走完才提交一次（服务端一人一天一条）。
 * 愿文只落本机 SharedPreferences，不上传；当前佛像从拜佛页配置读取，无配置用本地兜底。
 */
class EarlyPracticeFragment : TopPmFragment<EarlyPracticeVM>() {

    companion object {
        private const val WORSHIP_TARGET = 3
        private const val NIANFO_TARGET = 108
        //跟读速度：每字 0.5 秒
        private const val CHANT_CHAR_MS = 500L
        private const val VOW_MAX = 200
        private const val SP_NAME = "early_practice"
        private const val SP_KEY_VOW = "last_vow"
        private const val SP_KEY_VOW_HISTORY = "vow_history"
        //分步完成状态（按天存，跨天作废）：五步 json
        private const val SP_KEY_PROGRESS = "progress"
        private const val SP_KEY_PROGRESS_DATE = "progress_date"
        private val STEP_NAMES = intArrayOf(
            R.string.early_practice_step_worship,
            R.string.early_practice_step_chant,
            R.string.early_practice_step_nianfo,
            R.string.early_practice_step_vow,
            R.string.early_practice_step_share
        )
    }

    override fun layoutId() = R.layout.fragment_early_practice

    override fun initVM(): EarlyPracticeVM = provideVM()

    private val binding get() = mBinding as FragmentEarlyPracticeBinding

    private lateinit var pagerAdapter: EarlyPracticePagerAdapter
    private lateinit var wb: ItemEpWorshipBinding
    private lateinit var cb: ItemEpChantBinding
    private lateinit var nb: ItemEpNianfoBinding
    private lateinit var vb: ItemEpVowBinding
    private lateinit var rb: ItemEpReviewBinding

    //当前步骤 1..5
    private var step = 1

    private var worshipCount = 0
    private var bowing = false
    //礼佛双手按住：按 pointerId 跟踪左右手指，多指分发不会错位
    private var leftPointerId = -1
    private var rightPointerId = -1
    private var handsHeld = false
    private lateinit var tilt: EpTiltDetector

    private var chantStarted = false
    private var chantPaused = false
    private var chantPos = 0
    private var chantDone = false
    private var chantElapsed = 0
    private lateinit var chantIndices: List<Int>

    private var nianfoCount = 0
    private var nianfoDone = false
    private var strikeAnim: ObjectAnimator? = null
    private var feedbackAnim: ObjectAnimator? = null
    private val rhythm = NianfoRhythm()
    private val woodSound = NianfoWoodSound()

    private var vowDone = false
    private lateinit var vowHistory: MutableList<String>

    //进页时间，用于上报总时长
    private var enterTime = 0L

    //提交成功后等统计回来再渲染结果页，避免连续天数显示成旧值
    private var justSubmitted = false
    private var todayDone = false
    //服务端返回的本次早课修行值（提交结果 data），结果页展示用
    private var expGain = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val chantRunnable = object : Runnable {
        override fun run() {
            if (!isAdded || chantDone) return
            chantPos++
            chantElapsed = chantPos / 2
            renderSutra()
            if (chantPos >= chantIndices.size) {
                chantDone = true
                saveProgress()
                cb.tvChantProgress.text = getString(R.string.ep_chant_done)
                renderHeader()
                renderButton()
            } else {
                mainHandler.postDelayed(this, CHANT_CHAR_MS)
            }
        }
    }

    private val sp by lazy {
        requireContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack, binding.tvAction,
        binding.tvTab1, binding.tvTab2, binding.tvTab3, binding.tvTab4, binding.tvTab5
    )

    override fun initShiTu() {
        enterTime = System.currentTimeMillis()
        //禁止手势滑动，只能点底部 tab 切换
        binding.vpSteps.isUserInputEnabled = false
        bindPages()
        bindWorship()
        bindNianfo()
        bindVow()
        tilt = EpTiltDetector(requireContext()) { onBow() }

        binding.vpSteps.registerOnPageChangeCallback(pageChangeCallback)

        restoreProgress()
        renderHeader()
        renderButton()
        if (GlobalConfig.session.isNotBlank()) {
            postOnEnterAnimationEnd { mViewModel.loadStatistic() }
        }
    }

    /** 五页一次建好，直接拿各页 view 绑 ViewBinding */
    private fun bindPages() {
        pagerAdapter = EarlyPracticePagerAdapter(binding.vpSteps)
        binding.vpSteps.adapter = pagerAdapter
        binding.vpSteps.offscreenPageLimit = 4
        wb = ItemEpWorshipBinding.bind(pagerAdapter.pageView(0))
        cb = ItemEpChantBinding.bind(pagerAdapter.pageView(1))
        nb = ItemEpNianfoBinding.bind(pagerAdapter.pageView(2))
        vb = ItemEpVowBinding.bind(pagerAdapter.pageView(3))
        rb = ItemEpReviewBinding.bind(pagerAdapter.pageView(4))

        //诵经：把心经的非空白字符抽成逐字引导序列
        val sutra = getString(R.string.ep_heart_sutra)
        chantIndices = sutra.indices.filter { !sutra[it].isWhitespace() }.toList()
        renderSutra()

        //礼佛页佛像：与拜佛页同源，取配置里的当前佛像，失败保持本地兜底
        val buddhaUrl = GlobalConfig.globalParam?.buddhaConfig?.firstOrNull()?.let {
            it.buddhaCoverUrl?.takeIf { s -> s.isNotBlank() }
                ?: it.buddhaPicture?.takeIf { s -> s.isNotBlank() }
        }
        if (!buddhaUrl.isNullOrBlank()) {
            com.bumptech.glide.Glide.with(wb.ivBuddha).load(buddhaUrl).into(wb.ivBuddha)
        }
        wb.tvWorshipCount.text = getString(R.string.ep_worship_count, 0)
        nb.tvNianfoCount.text = getString(R.string.ep_nianfo_count, 0)
    }

    // ==================== 第一步：礼佛 ====================

    private fun bindWorship() {
        wb.ivMonk.post { wb.ivMonk.pivotY = wb.ivMonk.height.toFloat() }
        //双指落在两个独立 View 上时，第二个手指的 ACTION_POINTER_DOWN 会按 pointerId
        //分发给第一个 touch target，各自 setOnTouchListener 收不全。改在父容器统一接。
        wb.llHands.setOnTouchListener { _, event -> onHandsTouch(event) }
    }

    /**
     * 双手按住区统一触摸分发。
     * 关键点：ACTION_POINTER_DOWN / ACTION_POINTER_UP 的 pointer 在 actionIndex 里，
     * 用 getPointerId(actionIndex) 区分左右手；同时第一时间禁止 ViewPager2 拦截，
     * 否则手指微动就被判成滑动，子 View 收到 ACTION_CANCEL，按住状态被清掉。
     */
    private fun onHandsTouch(event: MotionEvent): Boolean {
        val view = wb.llHands
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                //第一根手指落下就抢占拦截权，避免后续 MOVE 被 ViewPager2 吃掉
                binding.vpSteps.requestDisallowInterceptTouchEvent(true)
                updateHandByPointer(event.getPointerId(0), event.getX(0), event.getY(0), view)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                updateHandByPointer(event.getPointerId(i), event.getX(i), event.getY(i), view)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                removeHandByPointer(event.getPointerId(event.actionIndex))
            }
            MotionEvent.ACTION_UP -> {
                removeHandByPointer(event.getPointerId(0))
                binding.vpSteps.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                leftPointerId = -1
                rightPointerId = -1
                syncHandsHeld()
                binding.vpSteps.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_MOVE -> {
                //按住期间手指移动，重新按坐标校正左右归属（防止一根手指滑到另一边）
                for (i in 0 until event.pointerCount) {
                    updateHandByPointer(event.getPointerId(i), event.getX(i), event.getY(i), view)
                }
            }
        }
        return true
    }

    /** 按手指坐标判断落在左/右水波上，写入对应 pointerId */
    private fun updateHandByPointer(pointerId: Int, x: Float, y: Float, container: View) {
        val pad = 20f
        val inLeft = wb.vRippleLeft.let {
            x >= it.left - pad && x <= it.right + pad && y >= it.top - pad && y <= it.bottom + pad
        }
        val inRight = wb.vRippleRight.let {
            x >= it.left - pad && x <= it.right + pad && y >= it.top - pad && y <= it.bottom + pad
        }
        if (inLeft && !inRight) {
            leftPointerId = pointerId
            if (rightPointerId == pointerId) rightPointerId = -1
        } else if (inRight && !inLeft) {
            rightPointerId = pointerId
            if (leftPointerId == pointerId) leftPointerId = -1
        }
        syncHandsHeld()
    }

    private fun removeHandByPointer(pointerId: Int) {
        if (leftPointerId == pointerId) leftPointerId = -1
        if (rightPointerId == pointerId) rightPointerId = -1
        syncHandsHeld()
    }

    /** 左右手都在 → 停水波、开传感器；缺一手 → 恢复水波、停传感器 */
    private fun syncHandsHeld() {
        val both = leftPointerId >= 0 && rightPointerId >= 0 &&
            worshipCount < WORSHIP_TARGET && !bowing && !todayDone
        if (both == handsHeld) return
        handsHeld = both
        if (both) {
            wb.vRippleLeft.stopRipple()
            wb.vRippleRight.stopRipple()
            wb.tvWorshipTip.setText(R.string.ep_worship_tip_holding)
            tilt.start()
        } else {
            releaseHands()
        }
    }

    /** 松手/切页/退页：停传感器、恢复水波 */
    private fun releaseHands() {
        leftPointerId = -1
        rightPointerId = -1
        handsHeld = false
        if (!::tilt.isInitialized) return
        tilt.stop()
        if (!::wb.isInitialized) return
        if (worshipCount < WORSHIP_TARGET && !bowing) {
            wb.vRippleLeft.startRipple()
            wb.vRippleRight.startRipple()
            wb.tvWorshipTip.setText(R.string.ep_worship_tip)
        }
    }

    /** 前倾 30° 触发一拜：小沙弥 2 秒俯身动画，结束计 1 次 */
    private fun onBow() {
        if (bowing || worshipCount >= WORSHIP_TARGET || !isAdded) return
        bowing = true
        tilt.stop()
        wb.tvWorshipTip.setText(R.string.ep_worship_tip_bowing)
        val monk = wb.ivMonk
        val set = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(monk, View.ROTATION_X, 0f, 26f, 0f),
                ObjectAnimator.ofFloat(monk, View.TRANSLATION_Y, 0f, 12f, 0f),
                ObjectAnimator.ofFloat(monk, View.SCALE_Y, 1f, 0.88f, 1f)
            )
            duration = 2000L
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isAdded) return
                    bowing = false
                    worshipCount++
                    saveProgress()
                    wb.tvWorshipCount.text = getString(R.string.ep_worship_count, worshipCount)
                    if (worshipCount >= WORSHIP_TARGET) {
                        wb.tvWorshipTip.setText(R.string.ep_worship_done)
                        releaseHands()
                    } else {
                        wb.tvWorshipTip.setText(R.string.ep_worship_tip_holding)
                        //拜完这一拜：若手还按着就重新武装传感器，松手了则停在水波静止态
                        handsHeld = false
                        syncHandsHeld()
                    }
                    renderHeader()
                    renderButton()
                }
            })
            start()
        }
        //持有引用避免被优化掉（AnimatorSet 由系统 Choreographer 驱动）
        bowAnim = set
    }

    private var bowAnim: AnimatorSet? = null

    // ==================== 第二步：诵经 ====================

    private fun startChant() {
        if (chantDone || chantStarted) return
        chantStarted = true
        chantPaused = false
        mainHandler.removeCallbacks(chantRunnable)
        mainHandler.postDelayed(chantRunnable, CHANT_CHAR_MS)
        renderButton()
    }

    /** KTV 式逐字高亮：已读金色、未读暗金、当前字带柔光底 */
    private fun renderSutra() {
        val sutra = getString(R.string.ep_heart_sutra)
        val text = SpannableStringBuilder(sutra)
        val dim = Color.parseColor("#6B5A3E")
        val gold = Color.parseColor("#E8CF94")
        text.setSpan(ForegroundColorSpan(dim), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val readEnd = if (chantPos <= 0) 0 else chantIndices[chantPos - 1] + 1
        if (readEnd > 0) {
            text.setSpan(ForegroundColorSpan(gold), 0, readEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (chantPos < chantIndices.size) {
            val idx = chantIndices[chantPos]
            text.setSpan(
                BackgroundColorSpan(Color.parseColor("#66E8CF94")),
                idx, idx + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        cb.tvSutraText.text = text
        cb.tvChantProgress.text = getString(R.string.ep_chant_progress, chantPos, chantIndices.size)
        //跟着高亮字滚动，保证引导始终在可视区
        cb.svSutra.post {
            if (!isAdded) return@post
            val layout = cb.tvSutraText.layout ?: return@post
            val line = layout.getLineForOffset(readEnd.coerceAtMost(sutra.length))
            cb.svSutra.scrollTo(0, (layout.getLineTop(line) - cb.svSutra.height / 2).coerceAtLeast(0))
        }
    }

    // ==================== 第三步：念佛 ====================

    private fun bindNianfo() {
        nb.vMala.onBeadTap = { onNianfoHit() }
        nb.vMala.onBeadTurn = { onNianfoHit() }
        nb.ivMuyu.setOnClickListener { onNianfoHit() }
    }

    private fun onNianfoHit() {
        if (nianfoDone || todayDone) return
        val (grade, _) = rhythm.judge()
        woodSound.hit(grade.startsWith("净念"))
        nianfoCount++
        saveProgress()
        nb.vCountFrame.visibility = View.VISIBLE
        nb.vCountFrame.text = "一藏 · $nianfoCount"
        nb.vCountFrame.reset()
        nb.tvNianfoCount.text = getString(R.string.ep_nianfo_count, nianfoCount)
        nb.vMala.advance()
        nb.tvFeedback.text = grade
        showNianfoFeedback()
        strikeMuyu()
        if (nianfoCount >= NIANFO_TARGET) {
            nianfoDone = true
            saveProgress()
            nb.tvNianfoCount.setText(R.string.ep_nianfo_done)
            renderHeader()
            renderButton()
        }
    }

    /** 木鱼敲击形变：与首页念佛页同一节奏，缩放支点对准鱼身而非画布中心 */
    private fun strikeMuyu() {
        val muyu = nb.ivMuyu
        if (muyu.pivotX == muyu.width / 2f && muyu.pivotY == muyu.height / 2f) {
            muyu.pivotX = muyu.width * 0.5f
            muyu.pivotY = muyu.height * (158f / 280f)
        }
        strikeAnim?.cancel()
        strikeAnim = ObjectAnimator.ofPropertyValuesHolder(
            muyu,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.955f, 1.025f, 0.99f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.93f, 1.045f, 0.99f, 1f)
        ).apply { duration = 420L; start() }
    }

    private fun showNianfoFeedback() {
        feedbackAnim?.cancel()
        val tv = nb.tvFeedback
        tv.alpha = 1f
        tv.translationY = 0f
        feedbackAnim = ObjectAnimator.ofPropertyValuesHolder(
            tv,
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -36f)
        ).apply { duration = 700L; start() }
    }

    // ==================== 第四步：发愿 ====================

    private fun bindVow() {
        val raw = sp.getString(SP_KEY_VOW_HISTORY, "").orEmpty()
        vowHistory = try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
        vb.etVow.setText(sp.getString(SP_KEY_VOW, ""))
        //编辑区恰好占屏高 1/3
        vb.flVowEditor.post {
            val h = (resources.displayMetrics.heightPixels / 3f).toInt()
            vb.flVowEditor.layoutParams.height = h
            vb.flVowEditor.requestLayout()
        }
        renderVowCount()
        vb.etVow.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                vowDone = !s?.toString()?.trim().isNullOrBlank()
                renderVowCount()
                saveProgress()
                renderHeader()
                if (step == 4) renderButton()
            }
        })
    }

    private fun renderVowCount() {
        val len = vb.etVow.text?.toString()?.length ?: 0
        vb.tvVowCount.text = getString(R.string.ep_vow_count, len, VOW_MAX)
    }

    private fun renderVowHistory() {
        vb.llVowHistory.removeAllViews()
        vb.tvVowHistoryEmpty.visibility = if (vowHistory.isEmpty()) View.VISIBLE else View.GONE
        vowHistory.take(6).forEach { vow ->
            val tv = com.fingertip.baselib.view.MyTextView(requireContext()).apply {
                text = vow
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                textSize = 12f
                setTextColor(TopApplication.instance.getColor(BaseR.color.c_gold))
                setBackgroundResource(R.drawable.bg_home_pill_outline)
                setPadding(28, 18, 28, 18)
                setOnClickListener { vb.etVow.setText(vow) }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
            vb.llVowHistory.addView(tv, lp)
        }
    }

    private fun commitVow() {
        val vow = vb.etVow.text?.toString()?.trim().orEmpty()
        sp.edit().putString(SP_KEY_VOW, vow).apply()
        if (vow.isNotBlank()) {
            vowHistory.remove(vow)
            vowHistory.add(0, vow)
            val arr = JSONArray()
            vowHistory.take(10).forEach { arr.put(it) }
            sp.edit().putString(SP_KEY_VOW_HISTORY, arr.toString()).apply()
        }
        vowDone = true
    }

    // ==================== 步骤控制 ====================

    override fun initObserver() {
        super.initObserver()

        mViewModel.statisticResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val data = result.data
            if (!result.success || data == null) return@observe
            if (justSubmitted) {
                justSubmitted = false
                showDoneResult(data.currentStreak)
            } else if (data.lastPracticeDate == todayStr()) {
                todayDone = true
                showAlreadyResult()
            }
        }

        mViewModel.submitResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (!result.success) {
                if (result.errorMsg?.contains("签到") == true || result.errorMsg?.contains("今天") == true) {
                    todayDone = true
                    showAlreadyResult()
                } else {
                    renderButton()
                    ToastUtil.showMessage(result.errorMsg ?: getString(R.string.ep_submit_fail))
                }
                return@observe
            }
            //服务端把本次实际发放修行值放在提交结果 data 里带回
            expGain = result.data?.toIntOrNull() ?: 0
            justSubmitted = true
            mViewModel.loadStatistic()
        }
    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> goBack()
            binding.tvAction -> onActionClick()
            binding.tvTab1 -> switchTab(1)
            binding.tvTab2 -> switchTab(2)
            binding.tvTab3 -> switchTab(3)
            binding.tvTab4 -> switchTab(4)
            binding.tvTab5 -> switchTab(5)
        }
    }

    /** 底部 tab 切换（手势已禁，唯一切换入口） */
    private fun switchTab(target: Int) {
        if (todayDone || target == step) return
        releaseHands()
        binding.vpSteps.setCurrentItem(target - 1, false)
    }

    private fun goBack() {
        if (todayDone) {
            pop()
        } else if (step > 1) {
            binding.vpSteps.setCurrentItem(step - 2, true)
        } else {
            pop()
        }
    }

    private fun onActionClick() {
        if (todayDone) {
            pop()
            return
        }
        if (step == 2 && !chantStarted) {
            startChant()
            return
        }
        if (!currentStepDone()) {
            ToastUtil.showMessage(getString(R.string.ep_need_current))
            return
        }
        if (step == 5) {
            submit()
        } else {
            if (step == 4) commitVow()
            binding.vpSteps.setCurrentItem(step, true)
        }
    }

    private fun currentStepDone(): Boolean = stepDone(step)

    /** 某一步的完成条件：回向(5)要求前四步全完成，按钮才亮 */
    private fun stepDone(s: Int): Boolean = when (s) {
        1 -> worshipCount >= WORSHIP_TARGET
        2 -> chantDone
        3 -> nianfoDone
        4 -> vowDone && vb.etVow.text?.toString()?.trim().isNullOrBlank().not()
        5 -> stepDone(1) && stepDone(2) && stepDone(3) && stepDone(4)
        else -> false
    }

    private fun renderHeader() {
        binding.tvStep.text = getString(R.string.ep_step_of, step)
        binding.tvStepName.setText(STEP_NAMES[step - 1])
        val dots = listOf(binding.tvDot1, binding.tvDot2, binding.tvDot3, binding.tvDot4, binding.tvDot5)
        val tabs = listOf(binding.tvTab1, binding.tvTab2, binding.tvTab3, binding.tvTab4, binding.tvTab5)
        dots.forEachIndexed { index, dot ->
            val no = index + 1
            val done = no <= 5 && stepDoneOld(no)
            when {
                done -> {
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
            //底部 tab：完成=金色，当前=亮金加粗，未完成=灰
            tabs[index].setTextColor(
                TopApplication.instance.getColor(
                    when {
                        done || no == step -> BaseR.color.c_gold_light
                        else -> BaseR.color.c_text_hint2
                    }
                )
            )
            tabs[index].alpha = if (no == step) 1f else 0.82f
        }
    }

    /** 顶部完成点只看单步自身完成态（回向步以提交成功为准，故单步恒 false） */
    private fun stepDoneOld(s: Int): Boolean = when (s) {
        1 -> worshipCount >= WORSHIP_TARGET
        2 -> chantDone
        3 -> nianfoDone
        4 -> vowDone && vb.etVow.text?.toString()?.trim().isNullOrBlank().not()
        else -> todayDone
    }

    private fun renderButton() {
        if (todayDone) {
            setAction(true, getString(R.string.ep_back))
            return
        }
        val text = when (step) {
            2 -> if (chantStarted) getString(R.string.ep_next) else getString(R.string.ep_chant_start)
            5 -> getString(R.string.ep_finish)
            else -> getString(R.string.ep_next)
        }
        //诵经进行中强制禁用，其余按当前步完成条件
        val enabled = if (step == 2 && chantStarted) false else currentStepDone()
        setAction(enabled, text)
    }

    private fun setAction(enabled: Boolean, text: String) {
        binding.tvAction.text = text
        binding.tvAction.setBackgroundResource(
            if (enabled) R.drawable.bg_ep_primary else R.drawable.bg_ep_primary_off
        )
        binding.tvAction.setTextColor(
            if (enabled) TopApplication.instance.getColor(BaseR.color.c_text_on_gold)
            else TopApplication.instance.getColor(BaseR.color.c_text_second)
        )
    }

    /** 回向卡：汇总前四步 */
    private fun renderReview() {
        rb.tvReviewWorship.text = getString(R.string.ep_review_worship, worshipCount)
        rb.tvReviewChant.text = getString(R.string.ep_review_chant)
        rb.tvReviewNianfo.text = getString(R.string.ep_review_nianfo, nianfoCount)
        rb.tvReviewVow.text = getString(
            R.string.ep_review_vow,
            vb.etVow.text?.toString()?.trim().orEmpty().ifBlank { getString(R.string.ep_vow_hint) }
        )
    }

    // ==================== 提交与结果 ====================

    private fun submit() {
        if (GlobalConfig.session.isBlank()) {
            ToastUtil.showMessage(getString(R.string.ep_login_first))
            return
        }
        val vow = vb.etVow.text?.toString()?.trim().orEmpty()
        //分步完成状态随提交上报服务器：前四步各自完成与否 + 明细
        val steps = JSONObject().apply {
            put("worship", JSONObject().put("done", worshipCount >= WORSHIP_TARGET).put("count", worshipCount))
            put("chant", JSONObject().put("done", chantDone).put("seconds", chantElapsed))
            put("nianfo", JSONObject().put("done", nianfoDone).put("count", nianfoCount))
            put("vow", JSONObject().put("done", vow.isNotEmpty()).put("length", vow.length))
            put("share", JSONObject().put("done", true))
        }
        val stepData = HashMap<String, Any>().apply {
            put("worship", worshipCount)
            put("chantSeconds", chantElapsed)
            put("nianfo", nianfoCount)
            put("vowWritten", vow.isNotEmpty())
            put("steps", steps)
        }
        val totalSeconds = maxOf(((System.currentTimeMillis() - enterTime) / 1000).toInt(), 60)
        setAction(false, getString(R.string.ep_finish))
        mViewModel.submit(GsonUtils.toJson(stepData), totalSeconds)
    }

    private fun showDoneResult(streak: Int) {
        todayDone = true
        binding.vpSteps.visibility = View.GONE
        binding.llResult.visibility = View.VISIBLE
        binding.tvStep.text = ""
        binding.tvResultTitle.setText(R.string.ep_done_title)
        binding.tvResultDesc.text = getString(R.string.ep_done_desc, expGain, streak)
        val next = PracticeStatisticData.nextMilestone(streak)
        binding.tvResultMilestone.text = if (next > 0) getString(R.string.ep_done_milestone, next - streak) else ""
        setAction(true, getString(R.string.ep_back))
        listOf(binding.tvDot1, binding.tvDot2, binding.tvDot3, binding.tvDot4, binding.tvDot5).forEach {
            it.setBackgroundResource(R.drawable.bg_home_dot_gold)
            it.text = "✓"
            it.setTextColor(TopApplication.instance.getColor(BaseR.color.c_text_on_gold))
        }
    }

    private fun showAlreadyResult() {
        todayDone = true
        binding.vpSteps.visibility = View.GONE
        binding.llResult.visibility = View.VISIBLE
        binding.tvStep.text = ""
        binding.tvResultTitle.setText(R.string.ep_already_title)
        binding.tvResultDesc.setText(R.string.ep_already_desc)
        binding.tvResultMilestone.text = ""
        setAction(true, getString(R.string.ep_back))
    }

    private fun todayStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ==================== 分步状态本地持久化（按天） ====================

    /** 五步完成状态写本地；跨天自动作废。提交状态仍以服务器为准。 */
    private fun saveProgress() {
        if (!::vb.isInitialized) return
        val progress = JSONObject().apply {
            put("worship", worshipCount)
            put("chant", chantDone)
            put("nianfo", nianfoCount)
            put("vow", vb.etVow.text?.toString().orEmpty())
        }
        sp.edit()
            .putString(SP_KEY_PROGRESS, progress.toString())
            .putString(SP_KEY_PROGRESS_DATE, todayStr())
            .apply()
    }

    /** 进页恢复当天的分步状态：只恢复进度，是否完成仍由服务端统计判定 */
    private fun restoreProgress() {
        if (sp.getString(SP_KEY_PROGRESS_DATE, "") != todayStr()) return
        val raw = sp.getString(SP_KEY_PROGRESS, "").orEmpty()
        if (raw.isBlank()) return
        try {
            val p = JSONObject(raw)
            worshipCount = p.optInt("worship", 0).coerceIn(0, WORSHIP_TARGET)
            chantDone = p.optBoolean("chant", false)
            nianfoCount = p.optInt("nianfo", 0).coerceIn(0, NIANFO_TARGET)
            if (chantDone) chantPos = chantIndices.size
            nianfoDone = nianfoCount >= NIANFO_TARGET
            val vow = p.optString("vow", "")
            vb.etVow.setText(vow)
            vowDone = vow.trim().isNotBlank()
            wb.tvWorshipCount.text = getString(R.string.ep_worship_count, worshipCount)
            nb.tvNianfoCount.text = getString(R.string.ep_nianfo_count, nianfoCount)
            renderSutra()
        } catch (_: Exception) {
            //本地状态损坏就当新的一天，不阻塞修持流程
        }
    }

    override fun onDestroyView() {
        //所有 binding / 动画清理必须在 super 之前：基类 onDestroyView 会先把 mBinding 置空
        mainHandler.removeCallbacksAndMessages(null)
        if (::tilt.isInitialized) tilt.stop()
        bowAnim?.cancel()
        bowAnim = null
        strikeAnim?.cancel()
        strikeAnim = null
        feedbackAnim?.cancel()
        feedbackAnim = null
        binding.vpSteps.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }

    private val pageChangeCallback = object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            step = position + 1
            if (step != 1) releaseHands()
            syncChantPlayback()
            renderHeader()
            renderButton()
            if (step == 4) renderVowHistory()
            if (step == 5) renderReview()
        }
    }

    /**
     * 诵经跟随页面：切到别的 step 暂停推进（保留 chantPos 断点），
     * 切回诵经页且仍在读时从下一个字继续，不重头、不加速、不叠多个回调。
     */
    private fun syncChantPlayback() {
        if (chantDone || !chantStarted) return
        if (step != 2) {
            if (!chantPaused) {
                mainHandler.removeCallbacks(chantRunnable)
                chantPaused = true
            }
        } else if (chantPaused) {
            chantPaused = false
            mainHandler.removeCallbacks(chantRunnable)
            mainHandler.postDelayed(chantRunnable, CHANT_CHAR_MS)
        }
    }
}

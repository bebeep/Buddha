package com.fingertip.uilib.fragment.nianfo

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.lifecycle.Observer
import com.fingertip.baselib.bean.BgMusic
import com.fingertip.baselib.bean.NianfoToday
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentNianfoBinding
import com.fingertip.uilib.dialog.WishRuleDialog
import com.fingertip.uilib.view.nianfo.NianfoRhythm
import com.fingertip.uilib.view.nianfo.NianfoWoodSound
import com.fingertip.uilib.viewmodel.NianFoVM
import kotlin.random.Random

/**
 * 念佛 · 静心（对应 ui设计/念佛页/念佛页面.html 原生重写）
 *
 * - 禅盘：呼吸环按节奏时钟缩放，拍点窗口点亮；18 珠佛珠环随敲击顺时针推进旋转，
 *   高亮光圈固定在正上方指示"念到哪了"
 * - 敲击木鱼/佛珠：按「距拍点毫秒数」判定 净念+2 / 专注+1 / 随念+1（浮字反馈）
 * - 一藏计数：初始"一藏"，念佛中"一藏·N"(1..108)；首次敲击后文案被圆角线框进度条包围；
 *   停止叩击 1.5s 后进度条排空，数字归 0 消失回"一藏"；期间继续念佛则取消排空继续累加
 * - 修行值结算在服务端：每满 108 声上报一次（practice/nianfoRoundReport），
 *   返回本次实际增加的修行值并弹"修行值+N"；进页拉当日已获得修行值，右上角展示 X/上限
 * - 梵音：曲库来自 globalParam.nianfoMusic（buddha_bg_music 表），每次进页随机取一首循环播放
 */
class NianFoFragment : TopPmFragment<NianFoVM>() {

    companion object {
        //一藏 = 108 念
        private const val BEADS_PER_ROUND = 108
        //停止念佛后的静止期：边框保持完整，期间继续念佛则重置
        private const val IDLE_HOLD_MS = 3000L
        //静止期结束后，边框排空（倒计时归零）时长
        private const val IDLE_DRAIN_MS = 3000L
    }

    override fun layoutId(): Int = R.layout.fragment_nianfo

    override fun initVM(): NianFoVM = provideVM()

    private val binding get() = mBinding as FragmentNianfoBinding

    private val rhythm = NianfoRhythm()
    private val woodSound = NianfoWoodSound()

    //本轮计数（1..108，每满一藏上报后归零）
    private var roundCount = 0
    //一藏上报在途：返回前保持"N藏·108"，期间敲击直接开新一轮
    private var pendingReport = false
    //当日已获得修行值（进页拉取，上报成功后本地累加）
    private var todayExp = 0
    //当日已完成轮数（进页拉取，上报成功后本地 +1；当前显示"第 todayRounds+1 藏"）
    private var todayRounds = 0
    //奖励强提示显示中：占用轮次计数位置，期间计数胶囊隐藏
    private var rewardShowing = false

    //停止叩击 1.5s 排空进度条（drain 动画本身即倒计时）
    private val mainHandler = Handler(Looper.getMainLooper())

    //本次进入随机选中的梵音（曲库为空则为 null，播放键置灰）
    private var music: BgMusic? = null
    private var player: MediaPlayer? = null
    private var playPending = false

    //音效动画（快速连击时需取消重开）
    private var strikeAnim: ObjectAnimator? = null
    private var feedbackAnim: ObjectAnimator? = null

    private val roundReportObserver = Observer<com.fingertip.baselib.viewmodel.RequestResult<Int>> {
        if (!isAdded) return@Observer
        pendingReport = false
        val data = it.data
        if (it.success && data != null) {
            todayExp += data
            //轮数按服务器记录口径累加（达上限的轮次服务器也落 0 记录）
            todayRounds++
            renderTodayExp()
            //一藏圆满强提示：轮次文本下方/佛珠上方（0 = 当日已达上限，换说明文案）
            if (data > 0) {
                showReward(getString(R.string.nianfo_round_done, data))
            } else {
                showReward(getString(R.string.nianfo_round_capped))
            }
        } else {
            ToastUtil.showMessage(getString(R.string.nianfo_round_report_fail))
        }
        //期间用户没有开新一轮才复位文案；已开新一轮则保持当前计数
        if (roundCount >= BEADS_PER_ROUND) {
            roundCount = 0
            renderRoundIdle()
        }
    }

    private val todayExpObserver = Observer<com.fingertip.baselib.viewmodel.RequestResult<NianfoToday>> {
        val data = it.data
        if (it.success && data != null) {
            todayExp = data.exp
            todayRounds = data.rounds
            renderTodayExp()
            //进页时若在空态，按当天已完成轮数刷新"第 N 藏"文案
            if (roundCount == 0 && !pendingReport) renderRoundIdle()
        }
    }

    override fun initShiTu() {
        binding.vBreath.rhythm = rhythm
        binding.vMala.onBeadTap = { doHit() }
        //右侧从上往下拨珠：只计数，不做时机判定/木鱼形变
        binding.vMala.onBeadTurn = { doTurn() }

        mViewModel.roundReportResult.observe(viewLifecycleOwner, roundReportObserver)
        mViewModel.todayExpResult.observe(viewLifecycleOwner, todayExpObserver)
        //进页拉当日已获得修行值，右上角展示 X/上限
        mViewModel.loadTodayExp()
        renderRoundIdle()
        renderTodayExp()

        pickMusic()
        binding.sbVolume.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                player?.setVolume(progress / 100f, progress / 100f)
            }

            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })
    }

    //region 一藏计数与修行值

    /** 统一计一声：更新文案/进度框，重置停止倒计时；满一藏即上报 */
    private fun addChant() {
        if (roundCount >= BEADS_PER_ROUND) {
            //上一轮还在结算中：直接开新一轮
            roundCount = 0
        }
        roundCount++
        if (roundCount >= BEADS_PER_ROUND) {
            pendingReport = true
            renderRoundActive()
            mViewModel.reportRound()
        } else {
            renderRoundActive()
        }
    }

    /** 念佛中：「N藏·m」(N=当天第几轮)，圆角矩形边框完整包围，并启动停止倒计时（边框排空动画） */
    private fun renderRoundActive() {
        binding.vCountFrame.text = roundLabel() + " · $roundCount"
        //强提示占用同一位置期间，计数胶囊保持隐藏（提示结束后再以最新计数恢复）
        binding.vCountFrame.visibility = if (rewardShowing) View.GONE else View.VISIBLE
        binding.vCountFrame.reset()
        if (roundCount < BEADS_PER_ROUND) {
            //停止念佛 3s 静止期 → 边框再花 3s 排空 → 归零隐藏（期间继续念佛整体重置）
            binding.vCountFrame.scheduleIdle(IDLE_HOLD_MS, IDLE_DRAIN_MS) {
                //排空结束：数字归 0 消失，回"N藏"
                roundCount = 0
                renderRoundIdle()
            }
        }
    }

    /** 空态：文案"N藏"（当天已完成 N-1 轮），边框隐藏 */
    private fun renderRoundIdle() {
        binding.vCountFrame.text = roundLabel()
        binding.vCountFrame.reset()
        binding.vCountFrame.visibility = View.GONE
    }

    /** 当前轮标签：当天已完成轮数 + 1，中文数字，如"一藏""二藏""十二藏" */
    private fun roundLabel(): String = "${cnNumeral(todayRounds + 1)}藏"

    /** 正整数转中文数字（1..99），超出范围回退阿拉伯数字 */
    private fun cnNumeral(n: Int): String {
        if (n !in 1..99) return n.toString()
        val digits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        val tens = n / 10
        val ones = n % 10
        return when {
            n < 10 -> digits[n]
            tens == 1 -> if (ones == 0) "十" else "十" + digits[ones]
            else -> digits[tens] + "十" + if (ones == 0) "" else digits[ones]
        }
    }

    private fun renderTodayExp() {
        val perDay = GlobalConfig.globalParam?.expPerdayInNianfo ?: 0
        binding.tvExpToday.text = "$todayExp/$perDay"
    }

    //endregion

    /** 每次进页随机取一首梵音（循环播放），曲库空则播放键置灰 */
    private fun pickMusic() {
        val list = GlobalConfig.globalParam?.nianfoMusic.orEmpty().filter { !it.musicUrl.isNullOrBlank() }
        music = if (list.isEmpty()) null else list[Random.nextInt(list.size)]
        val m = music
        if (m == null) {
            binding.tvTrackName.setText(R.string.nianfo_audio_default_title)
            binding.tvTrackSub.setText(R.string.nianfo_audio_empty)
            binding.flPlay.alpha = 0.4f
            return
        }
        binding.tvTrackName.text = m.musicTitle.orEmpty().ifBlank { getString(R.string.nianfo_audio_default_title) }
        binding.tvTrackSub.text = m.musicDesc.orEmpty().ifBlank { getString(R.string.nianfo_audio_ready) }
        //预加载：进页即准备，点播放无等待
        ensurePlayer()
    }

    private fun ensurePlayer() {
        if (player != null) return
        val m = music ?: return
        val url = m.musicUrl ?: return
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            isLooping = true
            setVolume(binding.sbVolume.progress / 100f, binding.sbVolume.progress / 100f)
            setOnPreparedListener { p ->
                if (playPending && isAdded) {
                    playPending = false
                    p.start()
                    renderPlaying(true)
                }
            }
            setOnErrorListener { _, _, _ ->
                ToastUtil.showMessage("梵音播放失败")
                renderPlaying(false)
                true
            }
            try {
                setDataSource(url)
                prepareAsync()
            } catch (e: Exception) {
                ToastUtil.showMessage("梵音播放失败")
            }
        }
    }

    private fun toggleMusic() {
        val m = music
        if (m == null) {
            ToastUtil.showMessage(getString(R.string.nianfo_audio_empty))
            return
        }
        val p = player
        if (p == null) {
            playPending = true
            ensurePlayer()
            return
        }
        try {
            if (p.isPlaying) {
                p.pause()
                renderPlaying(false)
            } else {
                p.start()
                renderPlaying(true)
            }
        } catch (e: Exception) {
            //prepare 未完成就点：挂起，等 onPrepared 自动播
            playPending = true
        }
    }

    private fun renderPlaying(playing: Boolean) {
        binding.ivPlay.setImageResource(if (playing) R.drawable.ic_nianfo_pause else R.drawable.ic_nianfo_play)
        binding.ivPlay.contentDescription =
            getString(if (playing) R.string.nianfo_audio_stop else R.string.nianfo_audio_play)
        binding.tvTrackSub.setText(if (playing) R.string.nianfo_audio_playing else R.string.nianfo_audio_paused)
    }

    /** 敲击：判定 → 佛珠推进 → 木鱼形变 → 涟漪 → 反馈浮字 → 音效 → 计数/上报 */
    private fun doHit() {
        val (grade, _) = rhythm.judge()
        binding.vMala.advance()
        strikeMuyu()
        showRipple()
        woodSound.hit(grade.startsWith("净念"))
        addChant()
        //满一藏时浮字让位给"修行值+N"（上报返回后弹）
        if (roundCount < BEADS_PER_ROUND) {
            showFeedback(grade)
            binding.tvHint.setText(R.string.nianfo_hint)
        } else {
            binding.tvHint.setText(R.string.nianfo_hint_round)
        }
    }

    /** 拨珠过一格：只计一声（不做判定、不敲木鱼形变） */
    private fun doTurn() {
        woodSound.hit(false)
        addChant()
        if (roundCount < BEADS_PER_ROUND) {
            binding.tvHint.setText(R.string.nianfo_hint)
        } else {
            binding.tvHint.setText(R.string.nianfo_hint_round)
        }
    }

    /** 木鱼敲击形变（对应原型 fishhit：1 → .955/.93 → 1.025/1.045 → .99 → 1） */
    private fun strikeMuyu() {
        val muyu = binding.ivMuyu
        veryFirstPivot(muyu)
        strikeAnim?.cancel()
        strikeAnim = ObjectAnimator.ofPropertyValuesHolder(
            muyu,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.955f, 1.025f, 0.99f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.93f, 1.045f, 0.99f, 1f)
        ).apply {
            duration = 420
            start()
        }
    }

    //木鱼 vector 的身体中心在 (160,158)/320x280，缩放 pivot 对齐到身体而非画布中心
    private fun veryFirstPivot(v: View) {
        if (v.pivotX == v.width / 2f && v.pivotY == v.height / 2f) {
            v.pivotX = v.width * 0.5f
            v.pivotY = v.height * (158f / 280f)
        }
    }

    /** 敲击涟漪：圆环从 .8 扩到 2.5 并渐隐（原型 .ripple 700ms） */
    private fun showRipple() {
        val size = (120 * resources.displayMetrics.density).toInt()
        val stage = binding.flStage
        val ring = View(requireContext()).apply {
            setBackgroundResource(R.drawable.bg_nianfo_ripple)
        }
        stage.addView(ring, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
        ring.scaleX = 0.8f
        ring.scaleY = 0.8f
        ring.alpha = 0.9f
        //end action 用捕获的 view，退出页面后动画收尾不再碰 binding（mBinding 已被基类置 null）
        ring.animate()
            .scaleX(2.5f).scaleY(2.5f).alpha(0f)
            .setDuration(700)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction { stage.removeView(ring) }
            .start()
    }

    /** 反馈浮字：淡入 → 上浮 → 渐隐（原型 fb 900ms） */
    private fun showFeedback(text: String) {
        val tv = binding.tvFeedback
        feedbackAnim?.cancel()
        tv.text = text
        val dy = 34 * resources.displayMetrics.density
        feedbackAnim = ObjectAnimator.ofPropertyValuesHolder(
            tv,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f, 1f, 0f),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 10f, -dy),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 1.05f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 1.05f, 1f)
        ).apply {
            duration = 900
            interpolator = LinearInterpolator()
            start()
        }
        //下一拍复位，避免浮字停在半空
        tv.postDelayed({
            if (feedbackAnim?.isRunning != true) tv.translationY = 0f
        }, 950)
    }

    /** 念佛规则弹窗（通用规则版式，正文含修行值奖励描述，数值取服务器下发配置） */
    private fun showRules() {
        val perRound = GlobalConfig.globalParam?.expInNianfo ?: 0
        val perDay = GlobalConfig.globalParam?.expPerdayInNianfo ?: 0
        WishRuleDialog(requireContext(), getString(R.string.nianfo_rules_title), getString(R.string.nianfo_rules_body, perRound, perDay)).show()
    }

    /** 一藏奖励强提示：占用轮次计数的槽位（纯金字无边框），弹入停留 3s 渐隐后恢复计数胶囊 */
    private fun showReward(text: String) {
        val tv = binding.tvReward
        rewardShowing = true
        //计数胶囊让位隐藏
        binding.vCountFrame.visibility = View.GONE
        //取消上一条还在排队的渐隐
        mainHandler.removeCallbacks(rewardFadeRunnable)
        tv.animate()?.cancel()
        tv.text = text
        tv.visibility = View.VISIBLE
        tv.alpha = 0f
        tv.scaleX = 0.72f
        tv.scaleY = 0.72f
        tv.translationY = 12f * resources.displayMetrics.density
        tv.animate()
            ?.alpha(1f)?.scaleX(1f)?.scaleY(1f)?.translationY(0f)
            ?.setDuration(280)
            ?.setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
            ?.start()
        //停留 3s 后渐隐
        mainHandler.postDelayed(rewardFadeRunnable, 3000)
    }

    private val rewardFadeRunnable = Runnable {
        //视图已销毁（mBinding 置 null）：直接返回，严禁再碰 binding
        val b = mBinding as? FragmentNianfoBinding ?: return@Runnable
        rewardShowing = false
        b.tvReward.animate()
            ?.alpha(0f)?.translationY(-10f * resources.displayMetrics.density)
            ?.setDuration(600)
            ?.setInterpolator(android.view.animation.AccelerateInterpolator())
            ?.withEndAction {
                if (mBinding === null) return@withEndAction
                b.tvReward.visibility = View.GONE
                //强提示退场：恢复计数胶囊——有在途计数显示最新次数，否则回"N藏"空态
                if (roundCount > 0) renderRoundActive() else renderRoundIdle()
            }
            ?.start()
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack, binding.ivInfo, binding.ivMuyu, binding.flPlay
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.ivInfo -> showRules()
            binding.ivMuyu -> doHit()
            binding.flPlay -> toggleMusic()
        }
    }

    override fun onDestroyView() {
        //基类 super.onDestroyView() 会把 mBinding 置 null，所有 binding 访问必须在 super 之前
        mainHandler.removeCallbacksAndMessages(null)
        strikeAnim?.cancel()
        feedbackAnim?.cancel()
        binding.vCountFrame.reset()
        binding.tvReward.animate()?.cancel()
        binding.tvFeedback.animate()?.cancel()
        super.onDestroyView()
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        player = null
    }
}

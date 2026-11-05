package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.HomeSummaryData
import com.fingertip.baselib.bean.LampStatusData
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.bean.TodayBuddhaInfo
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.utils.DailyQuotes
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemHomeBuddhaCardBinding
import com.fingertip.uilib.databinding.ItemHomeGameBinding
import com.fingertip.uilib.databinding.ItemHomeKingkongBinding
import com.fingertip.uilib.databinding.ItemHomeLampBarBinding
import com.fingertip.uilib.databinding.ItemHomeMomentBinding
import com.fingertip.uilib.databinding.ItemHomePracticeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R as BaseR

/**
 * 首页六区块适配器，一个 RecyclerView 多 viewType。
 *
 * 区块本身不带业务，只负责把 [summary] / [today] 渲染成卡片，
 * 点击一律回调给 HostFragment（跳页面、切 tab 都在那边做，adapter 不碰导航）。
 * 顺序与开关由 HostFragment 按服务端 blocks 拼好 [HomeSection] 列表后 initData 进来。
 */
class HomeSectionAdapter(context: Context) : TopRcAdapter<HomeSection, TopRcAdapter.TopRcViewHolder>(context) {

    companion object {
        //1 佛历头卡 2 今日修持 3 供灯状态条 4 金刚区 5 游戏卡 6 佛友圈精选 7 骨架
        const val TYPE_BUDDHA_CARD = 1
        const val TYPE_PRACTICE = 2
        const val TYPE_LAMP = 3
        const val TYPE_KINGKONG = 4
        const val TYPE_GAME = 5
        const val TYPE_MOMENT = 6
        const val TYPE_SKELETON = 7

        //签到进度环按 7 天一个小周期展示
        private const val SIGN_CYCLE_DAYS = 7f

        /**
         * 服务端 blockKey → viewType，key 与 home_block_config.blockKey 一致；
         * 不认识的 key 返回 0，调用方直接跳过（后端上新区块时老版本不至于崩）
         */
        fun typeOf(blockKey: String?): Int = when (blockKey) {
            "buddhaCard" -> TYPE_BUDDHA_CARD
            "practice" -> TYPE_PRACTICE
            "lampBar" -> TYPE_LAMP
            "kingkong" -> TYPE_KINGKONG
            "game" -> TYPE_GAME
            "momentFeed" -> TYPE_MOMENT
            else -> 0
        }
    }

    //首页聚合数据，六区块共用
    var summary: HomeSummaryData? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    //今日佛历，只有头卡用
    var today: TodayBuddhaInfo? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    //头卡（整卡）点击
    var onBuddhaCardClick: (() -> Unit)? = null

    //「开始早课」
    var onStartPractice: (() -> Unit)? = null

    //修持三格：0 诵经 1 抄经 2 签到
    var onPracticeSlotClick: ((slot: Int) -> Unit)? = null

    //供灯条点击，切拜佛 tab
    var onLampClick: (() -> Unit)? = null

    //金刚区：0 佛经 1 许愿 2 寺庙 3 念佛
    var onKingKongClick: ((index: Int) -> Unit)? = null

    //游戏卡：0 禅院 1 放生 2 功德币商店
    var onGameClick: ((index: Int) -> Unit)? = null

    //佛友圈精选「查看全部」
    var onMomentMoreClick: (() -> Unit)? = null

    //佛友圈精选：单张卡点击，进动态详情
    var onMomentClick: ((MomentEntity) -> Unit)? = null

    //佛友圈精选：禅语日签卡点击，弹本地日签详情
    var onQuoteClick: ((DailyQuotes.Quote) -> Unit)? = null

    override fun getItemViewType(position: Int): Int = get(position)?.type ?: TYPE_SKELETON

    override fun initLayoutId(viewType: Int): Int = when (viewType) {
        TYPE_BUDDHA_CARD -> R.layout.item_home_buddha_card
        TYPE_PRACTICE -> R.layout.item_home_practice
        TYPE_LAMP -> R.layout.item_home_lamp_bar
        TYPE_KINGKONG -> R.layout.item_home_kingkong
        TYPE_GAME -> R.layout.item_home_game
        TYPE_MOMENT -> R.layout.item_home_moment
        else -> R.layout.item_home_skeleton
    }

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_BUDDHA_CARD -> bindBuddhaCard(holder)
            TYPE_PRACTICE -> bindPractice(holder)
            TYPE_LAMP -> bindLamp(holder)
            TYPE_KINGKONG -> bindKingKong(holder)
            TYPE_GAME -> bindGame(holder)
            TYPE_MOMENT -> bindMoment(holder)
            else -> {
                //骨架不用绑数据
            }
        }
    }

    /**
     * 区块 1：佛历头卡。
     * 接口没回来（today 为空）时先铺本地公历与佛历年份，回来再覆盖，
     * 失败就一直用兜底值，不显示错误态——顶部塌一条比显示「加载失败」更难用
     */
    private fun bindBuddhaCard(holder: TopRcAdapter.TopRcViewHolder) {
        val binding = holder.getBinding<ItemHomeBuddhaCardBinding>()
        val info = today

        val buddhistYear = info?.buddhistYear?.takeIf { it > 0 }
            ?: (Calendar.getInstance().get(Calendar.YEAR) + 543)
        binding.tvBuddhistYear.text = "佛历 $buddhistYear 年"
        binding.tvDateBig.text = SimpleDateFormat("M月d日", Locale.getDefault()).format(Date())

        val mood = info?.moodText?.takeIf { it.isNotBlank() }
        binding.tvMood.text = mood.orEmpty()
        binding.tvMood.visibility = if (mood == null) View.INVISIBLE else View.VISIBLE

        val lunar = info?.lunarText?.takeIf { it.isNotBlank() } ?: info?.dateText
        binding.tvLunar.text = lunar.orEmpty()

        //签到环：连续天数按 7 天一个小周期绕，签满 7 天显示满环而不是回到 0
        //顶部签到状态同步：当天已签（聚合接口 signDone）显示「已签到」，否则显示「签到」引导
        val days = summary?.streakDays ?: 0
        val signDone = summary?.signDone == true
        val inCycle = (days % SIGN_CYCLE_DAYS.toInt()).toFloat()
        binding.rpvSign.apply {
            max = SIGN_CYCLE_DAYS
            progress = if (days > 0 && inCycle == 0f) SIGN_CYCLE_DAYS else inCycle
            text = context.getString(if (signDone) R.string.home_sign_done else R.string.home_sign_ring)
            subText = if (days > 0) context.getString(R.string.home_sign_ring_sub, days) else ""
        }

        binding.root.setOnClickListener { onBuddhaCardClick?.invoke() }
    }

    /**
     * 区块 2：今日修持。
     * 头部显示连签与今日早课状态；三格槽位按聚合接口的当日完成态渲染
     * （sutraDone / copyDone / signDone，服务端按用户时区算"今天"）
     */
    private fun bindPractice(holder: TopRcAdapter.TopRcViewHolder) {
        val binding = holder.getBinding<ItemHomePracticeBinding>()
        val data = summary

        //没数据（游客或接口还没回来）时不摆打卡态，只留引导
        if (data == null) {
            binding.tvStreak.text = ""
            binding.tvPracticeState.text = ""
        } else {
            binding.tvStreak.text = if (data.streakDays > 0) {
                context.getString(R.string.home_streak_days, data.streakDays)
            } else ""
            binding.tvPracticeState.setText(
                if (data.practiceDoneToday) R.string.home_practice_done_today
                else R.string.home_practice_todo_today
            )
        }

        binding.tvStartPractice.setOnClickListener { onStartPractice?.invoke() }

        bindPracticeSlot(binding.llSlot1, binding.tvSlot1Check, binding.tvSlot1State, 0, data?.sutraDone == true)
        bindPracticeSlot(binding.llSlot2, binding.tvSlot2Check, binding.tvSlot2State, 1, data?.copyDone == true)
        bindPracticeSlot(binding.llSlot3, binding.tvSlot3Check, binding.tvSlot3State, 2, data?.signDone == true)
    }

    /**
     * 单格槽位：已完成显示金勾与亮金状态字，未完成显示虚线底 + 禁用色
     *
     * @param done 服务端下发的当日完成态；游客（summary 为空）一律按未完成渲染
     */
    private fun bindPracticeSlot(
        container: View,
        check: View,
        state: com.fingertip.baselib.view.MyTextView,
        slot: Int,
        done: Boolean
    ) {
        check.visibility = if (done) View.VISIBLE else View.GONE
        container.setBackgroundResource(if (done) R.drawable.bg_home_inner else R.drawable.bg_home_dashed)
        state.setText(if (done) R.string.home_slot_done else R.string.home_slot_go)
        state.setTextColor(
            if (done) TopApplication.instance.getColor(BaseR.color.c_gold_light) else TopApplication.instance.getColor(BaseR.color.c_text_hint)
        )
        container.setOnClickListener { onPracticeSlotClick?.invoke(slot) }
    }

    /**
     * 区块 3：供灯状态条。三态只换背景与文案，点击都是去拜佛 tab
     */
    private fun bindLamp(holder: TopRcAdapter.TopRcViewHolder) {
        val binding = holder.getBinding<ItemHomeLampBarBinding>()
        val lamp = summary?.lamp() ?: LampStatusData()

        when {
            lamp.isNone() -> {
                //无灯：虚线框引导首购，不给按钮，整条都可点
                binding.llLamp.setBackgroundResource(R.drawable.bg_home_dashed)
                binding.ivLampIcon.alpha = 0.45f
                binding.tvLampTitle.setText(R.string.home_lamp_none)
                binding.tvLampTitle.setTextColor(TopApplication.instance.getColor(BaseR.color.c_text_main))
                binding.tvLampDesc.setText(R.string.home_lamp_none_desc)
                binding.tvLampAction.visibility = View.GONE
            }
            lamp.isDying() -> {
                //将熄（剩余 <= 3 小时）：金描边高亮 + 续燃按钮
                binding.llLamp.setBackgroundResource(R.drawable.bg_home_lamp_warn)
                binding.ivLampIcon.alpha = 1f
                binding.tvLampTitle.setText(R.string.home_lamp_dying)
                binding.tvLampTitle.setTextColor(TopApplication.instance.getColor(BaseR.color.c_gold_light))
                binding.tvLampDesc.setText(R.string.home_lamp_dying_desc)
                binding.tvLampAction.visibility = View.VISIBLE
                binding.tvLampAction.setText(R.string.home_lamp_relight)
            }
            else -> {
                //燃烧中：静默小字，只报剩余时长
                binding.llLamp.setBackgroundResource(R.drawable.bg_home_card_a)
                binding.ivLampIcon.alpha = 1f
                binding.tvLampTitle.setText(R.string.home_lamp_burning)
                binding.tvLampTitle.setTextColor(TopApplication.instance.getColor(BaseR.color.c_text_main))
                binding.tvLampDesc.text = context.getString(R.string.home_lamp_remain, lamp.remainHours)
                binding.tvLampAction.visibility = View.GONE
            }
        }

        binding.llLamp.setOnClickListener { onLampClick?.invoke() }
    }

    /**
     * 区块 4：金刚区。图标与文案都是本地静态的，这里只挂点击
     */
    private fun bindKingKong(holder: TopRcAdapter.TopRcViewHolder) {
        val binding = holder.getBinding<ItemHomeKingkongBinding>()
        binding.llKk1.setOnClickListener { onKingKongClick?.invoke(0) }
        binding.llKk2.setOnClickListener { onKingKongClick?.invoke(1) }
        binding.llKk3.setOnClickListener { onKingKongClick?.invoke(2) }
        binding.llKk4.setOnClickListener { onKingKongClick?.invoke(3) }
    }

    /**
     * 区块 5：游戏卡。功德币余额来自聚合接口（全站唯一货币），三张卡片的置灰/高亮是布局写死的
     */
    private fun bindGame(holder: TopRcAdapter.TopRcViewHolder) {
        val binding = holder.getBinding<ItemHomeGameBinding>()
        val coin = summary?.coin
        binding.tvMerit.text = if (coin == null) "" else context.getString(R.string.home_game_merit, coin)

        binding.llGame1.setOnClickListener { onGameClick?.invoke(0) }
        binding.llGame2.setOnClickListener { onGameClick?.invoke(1) }
        binding.llGame3.setOnClickListener { onGameClick?.invoke(2) }
    }

    /**
     * 区块 6：佛友圈精选（占位）。整块和「查看全部」都切到佛友圈 tab
     */
    private fun bindMoment(holder: TopRcAdapter.TopRcViewHolder) {
        val binding = holder.getBinding<ItemHomeMomentBinding>()
        // 卡片适配器跟着 holder 走：RecyclerView 复用 holder 时不能每次重建，
        // 否则横滑位置每次滚回首页都会跳回第一张
        val cardAdapter = (binding.rvMomentCards.adapter as? HomeMomentCardAdapter)
            ?: HomeMomentCardAdapter().also {
                it.onItemClick = { card ->
                    val entity = card.entity
                    val quote = card.quote
                    if (entity != null) {
                        onMomentClick?.invoke(entity)
                    } else if (quote != null) {
                        // 禅语兜底卡没有动态 id，弹本地日签详情，别把用户甩去列表
                        onQuoteClick?.invoke(quote)
                    } else {
                        onMomentMoreClick?.invoke()
                    }
                }
                binding.rvMomentCards.adapter = it
            }
        cardAdapter.submit(cardAdapter.build(summary?.featuredMoments))

        binding.tvMomentMore.setOnClickListener { onMomentMoreClick?.invoke() }
    }
}

/**
 * 首页区块。type 就是 adapter 的 viewType，HostFragment 按服务端 blocks 排好顺序后放进列表
 */
class HomeSection(val type: Int)

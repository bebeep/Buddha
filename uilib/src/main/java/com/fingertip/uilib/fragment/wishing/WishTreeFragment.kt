package com.fingertip.uilib.fragment.wishing

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Outline
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.fingertip.baselib.bean.WishRecordBean
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.lzlz.toplib.extention.toPx
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentWishTreeBinding
import com.fingertip.uilib.dialog.WishDetailDialog
import com.fingertip.uilib.dialog.WishRuleDialog
import com.fingertip.uilib.im.WishBus
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.WishVM
import com.bumptech.glide.Glide
import java.util.Calendar
import kotlin.random.Random

/**
 * 许愿树主页（改版）：
 * - 全屏许愿树背景，愿签牌挂在「树冠以下、主干两侧的横向树杈」上随风摆动（随机相位/幅度的 rotation 摆动）
 * - 绝不压中间竖直主干；树上最多展示 9 张，真实愿望不足 9 条时用不可点击的装饰牌补满
 * - 点真实愿签牌 → 弹窗查看公开愿望，可点赞、可祝福
 * - 底部「开始许愿」→ 愿望编辑页，提交成功回来后新愿签从底部飞上树
 */
class WishTreeFragment : TopPmFragment<WishVM>() {

    companion object {
        private const val TAG = "WishTreeFragment"

        //愿签牌宽度（dp）：bg_wish_card 竖长金边牌（约 1:2.5）。用户要求树上最多 9 张且明显偏小 → 32dp（再缩 1/3）。
        private const val TAG_WIDTH_DP = 32

        //树上愿签牌上限：真实愿望不足 9 条时，用装饰牌填充到 9 张（装饰牌不可点击）。
        private const val MAX_TAGS = 9

        //愿签牌挂点（相对全屏容器的宽高比例），
        //坐标点依次从左往右
        private val TAG_SLOTS = listOf(
            0.18f to 0.49f,
            0.27f to 0.52f,
            0.315f to 0.45f,
            0.42f to 0.48f,
            0.49f to 0.398f ,
            0.585f to 0.43f,
            0.65f to 0.515f,
            0.72f to 0.435f,
            0.83f to 0.49f
        )
    }

    override fun layoutId(): Int = R.layout.fragment_wish_tree

    override fun initVM(): WishVM = provideVM()

    private val binding get() = mBinding as FragmentWishTreeBinding

    //弹窗们，页面统一持有走 VM
    private var detailDialog: WishDetailDialog? = null
    private var ruleDialog: WishRuleDialog? = null

    //树上正在展示的愿望（id → bean），点赞回滚时定位用
    private val shownWishes = LinkedHashMap<Int, WishRecordBean>()

    //摆动动画集中管理，刷新树时统一取消
    private val swayAnimators = ArrayList<ObjectAnimator>()

    //是否今天已许愿，进页面时顺便拉我的第一条愿望判断
    private var wishedToday = false

    override fun initShiTu() {
        //许愿树数据带 loading HUD，等入栈动画放完再拉，转场不被抢帧
        postOnEnterAnimationEnd {
            mViewModel.loadWishTree()
            checkTodayWished()
        }
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.wishTreeResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                val list = result.data.orEmpty()
                renderTags(list)
                //愿签要常驻树上：不在这里批量标已读，点开愿签查看详情时才标记，
                //服务端下次拉取会换掉看过的
            } else {
                log(TAG, "许愿树获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "许愿树加载失败，请稍后再试")
            }
        }

        mViewModel.myWishResult.observe(viewLifecycleOwner) { result ->
            if (!result.success) return@observe
            val first = result.data?.firstOrNull() ?: return@observe
            wishedToday = isToday(first.createDate)
            renderPostButton()
        }

        mViewModel.blessResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                detailDialog?.onBlessSuccess()
                mViewModel.loadWishTree()
            } else {
                detailDialog?.onBlessFailed(result.errorMsg)
            }
        }

        mViewModel.likeResult.observe(viewLifecycleOwner) { result ->
            if (!result.success) {
                detailDialog?.onLikeFailed(result.errorMsg)
            }
            //成功时弹窗里已乐观更新，无需处理
        }
    }

    override fun onResume() {
        super.onResume()
        //编辑页提交成功带回来的新愿望：播放飞签动画挂上树
        WishBus.pendingNewWish?.let { wish ->
            WishBus.pendingNewWish = null
            flyNewTag(wish)
        }
    }

    /**
     * 渲染愿签牌：清空容器，按挂点布局并启动摆动。
     * 真实愿望最多取 MAX_TAGS 条；不足则用装饰牌补到 MAX_TAGS（装饰牌不可点击）。
     */
    private fun renderTags(wishes: List<WishRecordBean>) {
        binding.flTags.removeAllViews()
        cancelSway()
        shownWishes.clear()

        //真实愿望去重后最多 MAX_TAGS 条；其余挂点用装饰牌补满
        val real = wishes.distinctBy { it.id }.take(MAX_TAGS)
        val decorCount = MAX_TAGS - real.size
        real.forEach { shownWishes[it.id] = it }
        binding.tvEmpty.visibility = if (real.isEmpty()) View.VISIBLE else View.GONE

        //等容器完成布局再摆位（转场中容器未 layout 时宽高为 0）
        binding.flTags.doOnLayout {
            val cw = binding.flTags.width
            val ch = binding.flTags.height
            if (cw <= 0 || ch <= 0) return@doOnLayout
            val tagWidth = TAG_WIDTH_DP.toPx()

            //先放真实愿望（可点击）
            real.forEachIndexed { index, wish ->
                placeTag(createTagView(wish, clickable = true), index, cw, ch, tagWidth)
            }
            //再放装饰牌补位（不可点击）
            repeat(decorCount) { i ->
                placeTag(createTagView(decorWish(), clickable = false), real.size + i, cw, ch, tagWidth)
            }
        }
    }

    /**
     * 在指定挂点摆放一张愿签牌，并启动风摆
     */
    private fun placeTag(tagView: View, slotIndex: Int, cw: Int, ch: Int, tagWidth: Int) {
        val (fx, fy) = TAG_SLOTS[slotIndex]
        //少量随机抖动，避免每次一模一样
//        val jx = (Random.nextFloat() - 0.1f) * 0.03f
//        val jy = (Random.nextFloat() - 0.1f) * 0.02f
        val jx = 0f
        val jy = 0f
        binding.flTags.addView(tagView)
        tagView.measure(
            View.MeasureSpec.makeMeasureSpec(tagWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        tagView.layout(0, 0, tagView.measuredWidth, tagView.measuredHeight)
        val px = ((fx + jx) * cw).toInt() - tagView.measuredWidth / 2
        val py = ((fy + jy) * ch).toInt()
        tagView.translationX = px.coerceIn(8, (cw - tagView.measuredWidth - 8).coerceAtLeast(8)).toFloat()
        tagView.translationY = py.coerceAtLeast(8).toFloat()
        startSway(tagView)
    }

    /**
     * 装饰用愿签牌数据：匿名（显示默认头像），无真实内容，仅用于补满 9 个挂点，不可点击。
     */
    private fun decorWish(): WishRecordBean =
        WishRecordBean().apply { id = -1; isAnonymous = 1; userAvatar = null }

    /**
     * 创建愿签牌：整张 bg_wish_card 金边牌（自带挂绳/流苏）作牌面，
     * 牌面中央留白面板叠圆形头像（许愿者头像；匿名或没有头像则显示默认头像）。
     * 不再显示文字。顶部挂孔为摆动轴。
     * 宽度固定（牌面按比例自适应高度），避免 wrap_content 在手动 measure 下尺寸竞态。
     * @param clickable 是否可点击查看详情；装饰牌传 false（不可点击）
     */
    private fun createTagView(wish: WishRecordBean, clickable: Boolean): View {
        val ctx = requireContext()
        val tagWidth = TAG_WIDTH_DP.toPx()

        //容器：牌面 + 头像叠加
        val root = FrameLayout(ctx)
        root.layoutParams = FrameLayout.LayoutParams(tagWidth, FrameLayout.LayoutParams.WRAP_CONTENT)

        //牌面：bg_wish_card.png（adjustViewBounds 按原图 ~1:2.5 比例自适应高度）
        val card = ImageView(ctx)
        card.setImageResource(R.drawable.bg_wish_card)
        card.adjustViewBounds = true
        card.scaleType = ImageView.ScaleType.FIT_CENTER
        card.layoutParams = FrameLayout.LayoutParams(tagWidth, FrameLayout.LayoutParams.WRAP_CONTENT)
        root.addView(card)

        //头像：叠在牌面中央留白面板（图高约 41%~76% 区域中部）。
        //面板中心相对牌高约 0.585；头像直径取牌宽 50%。
        //确保「始终正圆」：用 ViewOutlineProvider 把头像视图裁成圆——无论 Glide 是否成功 circleCrop、
        //无论加载的是真实头像还是默认头像，都会被裁成正圆（clipToOutline 在绘制时按视图尺寸生效）。
        //再垫一张白色圆底，让头像在金边牌上更干净。
        val panelCenterRatio = 0.585f
        val avatarSize = (tagWidth * 0.50f).toInt()
        val avatar = ImageView(ctx)
        avatar.scaleType = ImageView.ScaleType.CENTER_CROP
        avatar.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        avatar.clipToOutline = true
        avatar.layoutParams = FrameLayout.LayoutParams(avatarSize, avatarSize).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = ((tagWidth * 2.5f) * panelCenterRatio - avatarSize / 2).toInt()
        }
        //匿名 或 无头像 → 默认头像；否则加载许愿者头像（已强制圆形裁切）
        val headUrl = if (wish.isAnonymous == 1 || wish.userAvatar.isNullOrEmpty()) null else wish.userAvatar
        Glide.with(ctx)
            .load(headUrl)
            .circleCrop()
            .placeholder(com.fingertip.baselib.R.mipmap.icon_default_head)
            .error(com.fingertip.baselib.R.mipmap.icon_default_head)
            .into(avatar)
        root.addView(avatar)

        //摆动轴：牌顶挂孔处
        root.pivotY = 0f
        //仅真实愿望牌可点击查看详情；装饰牌不可点击
        if (clickable) {
            root.setOnClickListener { showDetailDialog(wish) }
        }
        return root
    }

    /**
     * 启动风摆：随机幅度/时长/初相位，绕挂绳顶端左右摇
     */
    private fun startSway(tagView: View) {
        val amplitude = 3f + Random.nextFloat() * 4f          //3~7 度
        val duration = 1800L + Random.nextLong(1400)          //1.8~3.2s
        val animator = ObjectAnimator.ofFloat(tagView, "rotation", -amplitude, amplitude)
        animator.duration = duration
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.repeatMode = ObjectAnimator.REVERSE
        animator.startDelay = Random.nextLong(600)
        animator.interpolator = DecelerateInterpolator()
        animator.start()
        swayAnimators.add(animator)
    }

    private fun cancelSway() {
        swayAnimators.forEach { it.cancel() }
        swayAnimators.clear()
    }

    /**
     * 新愿望飞签：在空挂点建牌，从屏幕底部飞上去，落定后转风摆
     */
    private fun flyNewTag(wish: WishRecordBean) {
        shownWishes[wish.id] = wish
        binding.tvEmpty.visibility = View.GONE

        val usedSlots = TAG_SLOTS.size
        binding.flTags.doOnLayout {
            val cw = binding.flTags.width
            val ch = binding.flTags.height
            if (cw <= 0 || ch <= 0) return@doOnLayout
            //取一个随机挂点
            val (fx, fy) = TAG_SLOTS[Random.nextInt(usedSlots)]
            val tagView = createTagView(wish, clickable = true)
            //树上已满 9 张时，先移除最后一张（通常是装饰牌），保证总数不超上限
            if (binding.flTags.childCount >= MAX_TAGS) {
                binding.flTags.removeViewAt(binding.flTags.childCount - 1)
            }
            binding.flTags.addView(tagView)
            val tagWidth = TAG_WIDTH_DP.toPx()
            tagView.measure(
                View.MeasureSpec.makeMeasureSpec(tagWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            tagView.layout(0, 0, tagView.measuredWidth, tagView.measuredHeight)
            val targetX = (fx * cw).toInt() - tagView.measuredWidth / 2
            val targetY = (fy * ch).toInt()
            tagView.translationX = targetX.coerceIn(8, (cw - tagView.measuredWidth - 8).coerceAtLeast(8)).toFloat()
            //起点：屏幕底部之外，动画飞到挂点（translationY 回 0）
            tagView.translationY = (ch + 60 - targetY.coerceAtLeast(8)).toFloat()
            tagView.alpha = 0f
            tagView.scaleX = 0.7f
            tagView.scaleY = 0.7f

            val flyY = ObjectAnimator.ofFloat(tagView, "translationY", tagView.translationY, 0f)
            val flyA = ObjectAnimator.ofFloat(tagView, "alpha", 0f, 1f)
            val flyS = ObjectAnimator.ofFloat(tagView, "scaleX", 0.7f, 1f)
            val flyS2 = ObjectAnimator.ofFloat(tagView, "scaleY", 0.7f, 1f)
            val set = AnimatorSet()
            set.playTogether(flyY, flyA, flyS, flyS2)
            set.duration = 950
            set.interpolator = DecelerateInterpolator(1.4f)
            set.start()
            set.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    startSway(tagView)
                }
            })
        }
    }

    /**
     * 愿签牌点击：弹窗查看愿望，可点赞、可祝福
     */
    private fun showDetailDialog(wish: WishRecordBean) {
        //点开才算看过：标记已读，服务端下次拉取换掉这条
        mViewModel.readWishing(listOf(wish.id))
        val dialog = detailDialog ?: WishDetailDialog(requireContext()).also { detailDialog = it }
        dialog.render(wish)
        dialog.onSubmitBless = { wishingId, content ->
            mViewModel.blessWishing(wishingId, content)
        }
        dialog.onToggleLike = { wishingId ->
            mViewModel.likeWishing(wishingId)
        }
        dialog.show()
    }

    private fun showRuleDialog() {
        val dialog = ruleDialog ?: WishRuleDialog(requireContext()).also { ruleDialog = it }
        dialog.show()
    }

    /**
     * 顺便拉我的愿望第一条，判断今天是否已许愿（按钮置灰态）
     */
    private fun checkTodayWished() {
        mViewModel.loadMyWishes(0)
    }

    private fun isToday(createDate: String?): Boolean {
        if (createDate.isNullOrBlank()) return false
        val now = Calendar.getInstance()
        val datePart = createDate.substringBefore(' ')
        val today = "%04d-%02d-%02d".format(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH)
        )
        return datePart == today
    }

    private fun renderPostButton() {
        binding.tvPostWish.text = if (wishedToday) "今日已许愿，明日再来" else "开始许愿"
        binding.tvPostWish.alpha = if (wishedToday) 0.5f else 1f
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack,
        binding.tvRule,
        binding.tvMyWish,
        binding.tvMyBless,
        binding.tvPostWish
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvRule -> showRuleDialog()
            //压栈页面的 parentFragment 是 null，必须走 utils.openPage（向上找 MainFragment）
            binding.tvMyWish -> openPage(MyWishFragment())
            binding.tvMyBless -> openPage(MyBlessFragment())
            binding.tvPostWish -> {
                if (wishedToday) return
                openPage(WishEditFragment())
            }
        }
    }
}

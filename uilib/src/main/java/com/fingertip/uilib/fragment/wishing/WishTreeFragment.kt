package com.fingertip.uilib.fragment.wishing

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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
import java.util.Calendar
import kotlin.random.Random

/**
 * 许愿树主页（改版）：
 * - 全屏许愿树背景，愿签牌挂在树冠上随风摆动（随机相位/幅度的 rotation 摆动）
 * - 点愿签牌 → 弹窗查看公开愿望，可点赞、可祝福
 * - 底部「开始许愿」→ 愿望编辑页，提交成功回来后新愿签从底部飞上树
 */
class WishTreeFragment : TopPmFragment<WishVM>() {

    companion object {
        private const val TAG = "WishTreeFragment"

        //愿签牌挂点（树冠区域，相对容器宽高的比例），顺序即灌签顺序
        private val TAG_SLOTS = listOf(
            0.34f to 0.12f, 0.55f to 0.10f, 0.72f to 0.15f, 0.44f to 0.24f,
            0.64f to 0.26f, 0.24f to 0.28f, 0.80f to 0.30f, 0.50f to 0.36f,
            0.30f to 0.40f, 0.70f to 0.40f, 0.42f to 0.50f, 0.62f to 0.52f,
            0.78f to 0.50f, 0.28f to 0.52f, 0.50f to 0.60f, 0.68f to 0.60f
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
     * 渲染愿签牌：清空容器，按挂点布局并启动摆动
     */
    private fun renderTags(wishes: List<WishRecordBean>) {
        binding.flTags.removeAllViews()
        cancelSway()
        shownWishes.clear()
        wishes.forEach { shownWishes[it.id] = it }
        binding.tvEmpty.visibility = if (wishes.isEmpty()) View.VISIBLE else View.GONE

        //等容器完成布局再摆位（转场中容器未 layout 时宽高为 0）
        binding.flTags.doOnLayout {
            val cw = binding.flTags.width
            val ch = binding.flTags.height
            if (cw <= 0 || ch <= 0) return@doOnLayout
            //运行时 addView 不触发系统 traversal，手动 measure/layout；
            //bounds 固定在原点、显示位置用 translation 设定——
            //这样系统后续 traversal 重置 bounds 也不会破坏定位
            val tagWidth = 112.toPx()
            wishes.take(TAG_SLOTS.size).forEachIndexed { index, wish ->
                val (fx, fy) = TAG_SLOTS[index]
                //少量随机抖动，避免每次一模一样
                val jx = (Random.nextFloat() - 0.5f) * 0.03f
                val jy = (Random.nextFloat() - 0.5f) * 0.02f
                val tagView = createTagView(wish)
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
        }
    }

    /**
     * 创建愿签牌：红线 + 木牌（愿望内容），顶部中心为摆动轴。
     * 宽度固定（挂绳居中、木牌撑满），避免 wrap_content 在手动 measure 下尺寸竞态
     */
    private fun createTagView(wish: WishRecordBean): LinearLayout {
        val ctx = requireContext()
        val tagWidth = 112.toPx()
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.layoutParams = FrameLayout.LayoutParams(tagWidth, FrameLayout.LayoutParams.WRAP_CONTENT)

        //挂绳
        val string = View(ctx)
        string.layoutParams = LinearLayout.LayoutParams(3.toPx(), 16.toPx())
        string.setBackgroundColor(0xFF8A5A2B.toInt())
        root.addView(string)

        //木牌
        val plaque = LinearLayout(ctx)
        plaque.orientation = LinearLayout.VERTICAL
        plaque.setBackgroundResource(R.drawable.bg_wish_tag_plaque)
        plaque.setPadding(8.toPx(), 10.toPx(), 8.toPx(), 10.toPx())
        plaque.elevation = 3f
        plaque.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        root.addView(plaque)

        val tv = TextView(ctx)
        tv.text = wish.wishContent.orEmpty()
        tv.textSize = 11f
        tv.setTextColor(0xFF5B4632.toInt())
        tv.maxLines = 6
        tv.ellipsize = android.text.TextUtils.TruncateAt.END
        tv.gravity = Gravity.CENTER
        tv.includeFontPadding = false
        plaque.addView(
            tv,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        //摆动轴：挂绳顶端中心
        root.pivotY = 0f
        root.setOnClickListener { showDetailDialog(wish) }
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
            val tagView = createTagView(wish)
            binding.flTags.addView(tagView)
            val tagWidth = 112.toPx()
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

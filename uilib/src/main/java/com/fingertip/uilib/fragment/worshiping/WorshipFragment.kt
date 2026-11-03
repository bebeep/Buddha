package com.fingertip.uilib.fragment.worshiping

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.bean.LampDetailData
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.BuddhaPagerAdapter
import com.fingertip.uilib.databinding.FragmentWorshipBinding
import com.fingertip.uilib.dialog.WorshipDialog
import com.fingertip.uilib.fragment.MainFragment
import com.fingertip.uilib.fragment.shop.ShopFragment
import com.fingertip.uilib.viewmodel.LampVM
import com.fingertip.uilib.viewmodel.WorshipVM
import org.greenrobot.eventbus.Subscribe

/**
 * 拜佛页面（多佛像）
 *
 * 佛龛里不止一尊佛像：佛龛区域用 ViewPager2 承载，左右滑动手指跟手，
 * 首尾循环（最后一尊再往左滑回到第一尊）。每尊佛像展示自己的静态图
 * （2D 分层结构，动态光效留给预留的 Lottie 场景层），不再做 3D 渲染
 * ——产品形态是固定机位看佛像、无旋转/缩放交互，3D 只带来崩溃风险
 * 和几十 MB 的模型下载。
 * 上香（供奉）与供灯都作用在当前选中的佛像上：
 * - 供奉：佛像名旁「供奉」按钮 → 价目弹窗 → worshipBuddha
 * - 供灯：点灯/续燃进商城灯油分类，建单带当前佛像 id，发货即点亮在其佛前
 *
 * 选中状态持久化：本地 SP 即时落盘 + 登录后 account/saveLastBuddha 异步上报，
 * 再进页面恢复上次选中的佛像；换设备登录以服务端记录为准。
 */
class WorshipFragment : TopPmFragment<WorshipVM>() {

    companion object {
        private const val TAG = "WorshipFragment"
    }

    private val binding get() = mBinding as FragmentWorshipBinding

    /** 佛龛轮播适配器（无限循环） */
    private lateinit var pagerAdapter: BuddhaPagerAdapter

    /** 当前选中的佛像，null=列表还没就绪 */
    private var currentBuddha: BuddhaConfig? = null

    //供灯状态单独一个 VM：拜佛页的展示链路和供灯是两件事，混在一个 VM 里会很乱
    private val lampVM: LampVM by lazy { provideVM() }

    //灯在燃烧时的呼吸动画（替代在场景里硬编码灯位坐标）
    private var breathAnimator: ValueAnimator? = null

    /** 供奉弹窗，供奉成功后要关掉它 */
    private var worshipDialog: WorshipDialog? = null

    /** 轮播页选中回调，onDestroyView 时要反注册 */
    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val buddha = pagerAdapter.buddhaAt(position) ?: return
            if (buddha.id != currentBuddha?.id) {
                //滑动引起的选中：观察者里不许再回设轮播坐标——
                //onPageSelected 在拖动中途就会触发，此时回设会瞬移打断手势，
                //圈尾→圈首的衔接就是这么被搞断的（LiveData 主线程同步派发，标志位可靠）
                pagerDrivenSelection = true
                mViewModel.selectBuddha(buddha)
                pagerDrivenSelection = false
            }
        }
    }

    /** true=本次选中由轮播滑动引起，observer 不要反设 setCurrentItem */
    private var pagerDrivenSelection = false

    override fun layoutId(): Int = R.layout.fragment_worship

    override fun initVM(): WorshipVM = provideVM()

    override fun initShiTu() {
        initBuddhaScene()
        initBuddhaActions()
        initLampPanel()
        mViewModel.initBuddhaList()
    }

    /**
     * 信息头与供奉入口的点击。
     * 佛像切换不摆选择条：佛龛区域左右滑动循环切换（见 [initBuddhaScene]）。
     */
    private fun initBuddhaActions() {
        //佛像名/标签区点击进佛像详情（供奉、介绍、推荐经典都在详情里）
        binding.llBuddhaInfo.setOnClickListener { openBuddhaDetail() }
        //供奉（上香）：直接弹价目，不用先进详情页再绕一圈
        binding.tvWorshipAction.setOnClickListener { showWorshipDialog() }
    }

    /**
     * 佛龛轮播：ViewPager2 手指跟手滑动 + 无限循环。
     * 滑动到某页 = 选中该佛像（走 VM 的持久化链路）；VM 反向下发的选中
     * （恢复上次/列表就绪）再同步回 ViewPager 坐标，同尊自动跳过不会成环。
     */
    private fun initBuddhaScene() {
        pagerAdapter = BuddhaPagerAdapter().apply {
            imageLoader = { iv, buddha ->
                val url = buddha.buddhaCoverUrl?.takeIf { it.isNotBlank() }
                    ?: buddha.buddhaPicture?.takeIf { it.isNotBlank() }
                if (url != null) {
                    //关掉 Glide 自带 crossfade，轮播滑动本身就是过渡
                    Glide.with(iv).load(url).dontAnimate().into(iv)
                }
            }
        }
        binding.vpBuddha.adapter = pagerAdapter
        binding.vpBuddha.offscreenPageLimit = 1
        binding.vpBuddha.registerOnPageChangeCallback(onPageChangeCallback)
    }

    private fun openBuddhaDetail() {
        val buddha = currentBuddha ?: return
        (parentFragment as? MainFragment)?.start(BuddhaDetailFragment.newInstance(buddha))
    }

    /**
     * 提交佛像列表给轮播 + 预加载全部封面进 Glide 内存缓存：
     * 循环边界的页面是新绑的 ViewHolder，等加载会闪一帧空白——预加载后绑定即出图
     */
    private fun submitBuddhaList(list: List<BuddhaConfig>) {
        pagerAdapter.submit(list)
        val ctx = context ?: return
        list.forEach { buddha ->
            val url = buddha.buddhaCoverUrl?.takeIf { it.isNotBlank() }
                ?: buddha.buddhaPicture?.takeIf { it.isNotBlank() }
            if (url != null) {
                Glide.with(ctx).load(url).preload()
            }
        }
    }

    /**
     * 供奉（上香）：按当前佛像的价目选天数，服务端扣功德币。
     * 功德币在海外版保留为修行激励货币（早课/读经可得），充值通道已下线，无 IAP 合规问题。
     */
    private fun showWorshipDialog() {
        val buddha = currentBuddha ?: return
        if (GlobalConfig.session.isBlank()) {
            ToastUtil.showMessage(getString(R.string.shop_login_first))
            return
        }
        val dialog = WorshipDialog(requireContext())
        worshipDialog = dialog
        dialog.render(buddha)
        dialog.onSubmit = { days, _ ->
            mViewModel.worshipBuddha(buddha.id, days)
        }
        dialog.show()
    }

    /**
     * 底部面板：灯状态 + 装扮三格。
     * 点灯/续燃一律进商城灯油分类购买，服务端发货时自动落灯记录——
     * 不在这一页自己写「点灯」按钮，避免出现"点了但没付钱也点亮"的第二条路径。
     * 灯油订单带当前佛像 id：灯点亮在哪尊佛前，由下单那一刻选中的佛像决定。
     */
    private fun initLampPanel() {
        binding.tvLampAction.setOnClickListener { openShop(ShopDressConfig.TYPE_OIL) }
        binding.llLamp.setOnClickListener { openShop(ShopDressConfig.TYPE_OIL) }
        binding.llDressLamp.setOnClickListener { openShop(ShopDressConfig.TYPE_LAMP) }
        binding.llDressShrine.setOnClickListener { openShop(ShopDressConfig.TYPE_SHRINE) }
        binding.llDressOil.setOnClickListener { openShop(ShopDressConfig.TYPE_OIL) }
        renderLamp(null)
    }

    /**
     * 装扮/灯油都在商城买，这里只负责带品类跳过去。
     * 灯油要把目标佛像捎给商城：建单落库，验单发货时点亮在该佛像前。
     */
    private fun openShop(dressType: Int) {
        if (GlobalConfig.session.isBlank()) {
            ToastUtil.showMessage(getString(R.string.shop_login_first))
            return
        }
        (parentFragment as? MainFragment)?.start(
            ShopFragment.newInstance(dressType, currentBuddha?.id ?: 0)
        )
    }

    /**
     * 灯状态三态渲染：无灯（虚线框引导首购）/ 燃烧中（静默报剩余）/ 将熄（高价感高亮 + 续燃）
     *
     * @param lamp 为空表示未登录或接口没回来，按"无灯"渲染，不显示错误态
     */
    private fun renderLamp(lamp: LampDetailData?) {
        val data = lamp ?: LampDetailData()
        when {
            data.isNone() -> {
                binding.llLamp.setBackgroundResource(R.drawable.bg_home_dashed)
                binding.vLampDot.visibility = View.GONE
                stopBreath()
                binding.tvLampName.setText(R.string.home_lamp_none)
                binding.tvLampDesc.setText(R.string.home_lamp_none_desc)
                binding.tvLampAction.visibility = View.VISIBLE
                binding.tvLampAction.setText(R.string.worship_light_lamp)
            }
            data.isDying() -> {
                binding.llLamp.setBackgroundResource(R.drawable.bg_home_lamp_warn)
                binding.vLampDot.visibility = View.VISIBLE
                startBreath()
                binding.tvLampName.text = data.displayName()
                binding.tvLampDesc.setText(R.string.home_lamp_dying_desc)
                binding.tvLampAction.visibility = View.VISIBLE
                binding.tvLampAction.setText(R.string.worship_relight)
            }
            else -> {
                binding.llLamp.setBackgroundResource(R.drawable.bg_home_card_b)
                binding.vLampDot.visibility = View.VISIBLE
                startBreath()
                binding.tvLampName.text = data.displayName()
                binding.tvLampDesc.text = getString(R.string.home_lamp_remain, data.remainHours)
                //燃烧中不摆按钮：灯还好好的，天天喊续燃会被当广告无视
                binding.tvLampAction.visibility = View.GONE
            }
        }
    }

    /**
     * 灯火呼吸：0.35~1 之间往复，模拟灯芯明暗。
     * 只在灯亮着时跑，无灯时停掉，省一帧算一帧。
     */
    private fun startBreath() {
        if (breathAnimator?.isRunning == true) return
        breathAnimator = ValueAnimator.ofFloat(0.35f, 1f).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { binding.vLampDot.alpha = it.animatedValue as Float }
            start()
        }
    }

    private fun stopBreath() {
        breathAnimator?.cancel()
        breathAnimator = null
        binding.vLampDot.alpha = 1f
    }

    override fun initObserver() {
        super.initObserver()

        //佛像列表就绪（globalParam 为空时接口兜底回来的）：恢复上次选中的佛像
        mViewModel.buddhaConfigResult.observe(viewLifecycleOwner) { result ->
            val list = result.data
            if (result.success && !list.isNullOrEmpty()) {
                submitBuddhaList(list)
                if (currentBuddha == null) {
                    mViewModel.restoreOrFirst(list)
                }
            }
        }

        //切换佛像（轮播滑动或初始恢复）：更新信息头、按需同步轮播坐标、按佛像拉灯状态。
        //轮播滑动引起的选中（pagerDrivenSelection）只更新信息，绝不动轮播坐标；
        //只有初始恢复/列表就绪才 setCurrentItem，把轮播定位到中间一圈
        mViewModel.selectedBuddha.observe(viewLifecycleOwner) { buddha ->
            buddha ?: return@observe
            val sameBuddha = currentBuddha?.id == buddha.id
            currentBuddha = buddha
            //globalParam 直供的列表不走 buddhaConfigResult，这里兜底提交一次
            val list = GlobalConfig.globalParam?.buddhaConfig.orEmpty()
            if (list.isNotEmpty() && pagerAdapter.size() != list.size) {
                submitBuddhaList(list)
            }
            if (!sameBuddha) {
                renderBuddhaInfo(buddha)
            }
            if (!pagerDrivenSelection) {
                val target = pagerAdapter.globalPositionOf(buddha.id)
                if (target >= 0 && binding.vpBuddha.currentItem != target) {
                    binding.vpBuddha.setCurrentItem(target, false)
                }
            }
            if (GlobalConfig.session.isNotBlank()) {
                lampVM.loadStatus(buddha.id)
            } else {
                renderLamp(null)
            }
        }

        //灯状态：失败一律按"无灯"渲染，不在主视觉上摆错误提示
        lampVM.statusResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            renderLamp(if (result.success) result.data else null)
        }

        //供奉结果：成功关弹窗，失败恢复按钮等用户重试
        mViewModel.worshipResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.worship_offer_success))
                worshipDialog?.onWorshipSuccess()
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.worship_offer_fail))
                worshipDialog?.onWorshipFailed()
            }
        }
    }

    /** 佛像信息头：名字 + 庇佑标签。标签是最多两三个的短词，逐个塞小 chip */
    private fun renderBuddhaInfo(buddha: BuddhaConfig) {
        binding.tvBuddhaName.text = buddha.buddhaName ?: ""
        val tags = binding.llBuddhaTags
        tags.removeAllViews()
        buddha.buddhaTag?.filterNotNull()?.filter { it.isNotBlank() }?.take(3)?.forEach { tag ->
            val tv = com.fingertip.baselib.view.MyTextView(requireContext())
            tv.text = tag
            tv.textSize = 10f
            tv.setPadding(16, 4, 16, 4)
            tv.setTextColor(0xFFC4A265.toInt())
            tv.setBackgroundResource(R.drawable.bg_home_inner_gold)
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 8
            tags.addView(tv, lp)
        }
    }

    // ==================== 生命周期管理 ====================

    override fun onResume() {
        super.onResume()
        //每次回到这一页都刷一次灯状态：用户可能刚在商城买了灯油/换了灯具；
        //多佛像后按当前佛像查，切佛像时也会各自刷
        if (GlobalConfig.session.isNotBlank()) {
            lampVM.loadStatus(currentBuddha?.id ?: 0)
        }
    }

    override fun onDestroyView() {
        //呼吸动画持有 View 引用，必须在 binding 置空之前停掉
        stopBreath()
        binding.vpBuddha.unregisterOnPageChangeCallback(onPageChangeCallback)
        super.onDestroyView()
    }

    // ==================== EventBus ====================

    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when (event.what) {

        }
    }
}

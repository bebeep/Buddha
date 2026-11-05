package com.fingertip.uilib.fragment.worshiping

import android.view.View
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.bean.LampDetailData
import com.fingertip.baselib.bean.MyDressData
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.BuddhaPagerAdapter
import com.fingertip.uilib.databinding.FragmentWorshipBinding
import com.fingertip.uilib.dialog.WorshipDialog
import com.fingertip.uilib.fragment.MainFragment
import com.fingertip.uilib.fragment.shop.MyDressFragment
import com.fingertip.uilib.fragment.shop.ShopFragment
import com.fingertip.uilib.viewmodel.LampVM
import com.fingertip.uilib.viewmodel.ShopVM
import com.fingertip.uilib.viewmodel.WorshipVM
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R as BaseR

/**
 * 拜佛页面（多佛像）
 *
 * 佛龛里不止一尊佛像：佛龛区域用 ViewPager2 承载，左右滑动手指跟手，
 * 首尾循环（最后一尊再往左滑回到第一尊）。
 * 每页是完整分层场景（自下而上）：佛龛 + 佛龛动画 + 佛光 + 佛像 + 净瓶 +
 * 贡品 + 香烛 + 佛灯 + 灯焰，随轮播一体滑动（见 item_buddha_page.xml）。
 *
 * 场景与装扮的关系：
 * - 佛龛：每尊佛像有默认龛（buddha_config.shrineDressId），用户佩戴的龛覆盖默认；
 *   点击佛龛进装扮（已拥有→我的装扮，未拥有→商城佛龛分类）。佛龛单次购买永久有效，跨佛像复用。
 * - 佛灯：位于佛像底部两侧，最上层；灯焰随灯油剩余比例缩放（油越少火越小）；
 *   点击佛灯进装扮；无灯/将熄时点击直达灯油分类续燃。灯具同样永久有效、跨佛像复用。
 * - 香烛/贡品：不入装扮商店，与供佛绑定——priceField 每档带 incense/offering 档次，
 *   供佛中按档次渲染素材，香烛随供佛剩余时长变短（对容器 scaleY）。
 *
 * 选中状态持久化：本地 SP 即时落盘 + 登录后 account/saveLastBuddha 异步上报。
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

    //供灯状态单独一个 VM：场景里的灯焰渲染依赖它
    private val lampVM: LampVM by lazy { provideVM() }

    //我的装扮（持有+佩戴位）：佛龛/佛灯按佩戴渲染，点击跳转按持有分流
    private val dressVM: ShopVM by lazy { provideVM() }

    private var myDressData: MyDressData? = null

    /** 当前灯状态，null=未登录或接口没回来（按无灯渲染） */
    private var lampData: LampDetailData? = null

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
            pageBinder = { holder, buddha ->
                bindBuddhaPage(holder, buddha)
            }
        }
        binding.vpBuddha.adapter = pagerAdapter
        binding.vpBuddha.offscreenPageLimit = 1
        binding.vpBuddha.registerOnPageChangeCallback(onPageChangeCallback)
    }

    /**
     * 单页全分层绑定。所有素材都是 1080x2400 全画布透明 PNG，位置在服务端素材
     * 合成时定死，客户端只做满屏叠层；动画按 URL 异步加载，view.tag 记录当前
     * 请求的 URL 防 recycled 页面旧回调串台；失败静默隐藏。
     */
    private fun bindBuddhaPage(holder: BuddhaPagerAdapter.Holder, buddha: BuddhaConfig) {
        //佛像本体
        val url = buddha.buddhaCoverUrl?.takeIf { it.isNotBlank() }
            ?: buddha.buddhaPicture?.takeIf { it.isNotBlank() }
        if (url != null) {
            //关掉 Glide 自带 crossfade，轮播滑动本身就是过渡
            Glide.with(holder.iv).load(url).dontAnimate().into(holder.iv)
        }
        bindSceneLottie(holder.back, buddha.sceneAnimUrl)
        bindSceneLottie(holder.front, buddha.sceneFrontAnimUrl)

        //佛龛：佩戴的龛覆盖佛像默认龛；付费龛可带专属氛围动画
        val shrineCfg = wornConfig(ShopDressConfig.TYPE_SHRINE)
        bindSceneImage(holder.shrine, shrineCfg?.resUrl?.takeIf { it.isNotBlank() }
            ?: buddha.shrineResUrl?.takeIf { it.isNotBlank() })
        bindSceneLottie(holder.shrineAnim, shrineCfg?.sceneAnimUrl?.takeIf { it.isNotBlank() }
            ?: buddha.shrineAnimUrl?.takeIf { it.isNotBlank() })

        //贡品：分列香炉两侧，服务端按供佛档次拼好直出，未供佛为空
        bindSceneImage(holder.offering, buddha.offeringResUrl?.takeIf { it.isNotBlank() })
        //三根香插炉中：香杆随供佛剩余时长变短（比例交给适配器在 layout 后按炉口锚点落地，
        //这里拿不到控件宽高，直接设 scaleY 会因为 pivot 在 0 而从左上角缩），
        //香炉固定不缩放，z 序在香杆之上盖住香脚
        val incenseVisible = bindSceneImage(holder.incense, buddha.incenseResUrl?.takeIf { it.isNotBlank() })
        holder.incenseGroup.visibility = if (incenseVisible) View.VISIBLE else View.GONE
        holder.incenseRatio = if (incenseVisible && buddha.worshipTotalSec > 0) {
            (buddha.worshipLeftTime.toFloat() / buddha.worshipTotalSec).coerceIn(0.35f, 1f)
        } else 1f
        bindSceneImage(holder.burner, if (incenseVisible) buddha.incenseBurnerResUrl?.takeIf { it.isNotBlank() } else null)
        bindSceneLottie(holder.smokeAnim, buddha.incenseAnimUrl?.takeIf { it.isNotBlank() })

        //佛灯：两侧各一盏（同一素材，右侧镜像），最上层。
        //灯具常显（佩戴灯 > 服务端下发的当前灯/默认灯）：没点灯时也要看得见、
        //点得到，否则用户既进不了续油也进不了灯具装扮
        val lampNow = lampData
        val lampCfg = wornConfig(ShopDressConfig.TYPE_LAMP)
        val lampUrl = lampCfg?.resUrl?.takeIf { it.isNotBlank() }
            ?: lampNow?.lampResUrl?.takeIf { it.isNotBlank() }
        val lampShown = bindSceneImage(holder.lampLeft, lampUrl)
        if (lampShown) {
            //右灯是独立 ImageView，必须自己加载同一素材（只设 visibility 会一直是空图）
            holder.lampRight.visibility = View.VISIBLE
            holder.lampRight.scaleX = -1f //整画布镜像，灯从左侧翻到右侧
            Glide.with(holder.lampRight).load(lampUrl).dontAnimate().into(holder.lampRight)
        } else {
            holder.lampLeft.visibility = View.GONE
            holder.lampRight.visibility = View.GONE
        }
        holder.lampLeft.visibility = if (lampShown) View.VISIBLE else View.GONE
        //灯焰只在燃着时出现，随灯油剩余比例缩放（油越少火越小）；
        //pivot 与镜像在适配器 applyGeometry 里按画布锚点落地
        val burning = lampNow != null && !lampNow.isNone()
        val flameVisible = lampShown && burning && bindSceneLottie(holder.flameLeft,
            lampCfg?.sceneAnimUrl?.takeIf { it.isNotBlank() } ?: lampNow?.lampAnimUrl?.takeIf { it.isNotBlank() })
        holder.flameRight.visibility = if (flameVisible) View.VISIBLE else View.GONE
        if (!flameVisible) {
            holder.flameLeft.cancelAnimation()
            holder.flameRight.cancelAnimation()
        }
        holder.flameScale = if (flameVisible) lampNow?.flameScale() ?: 1f else 1f

        //场景点击：佛龛→装扮(佛龛)；佛灯→无灯/将熄先续油，正常状态进灯具装扮
        holder.onShrineClick = { openDressPage(ShopDressConfig.TYPE_SHRINE) }
        holder.onLampClick = {
            if (lampNow == null || lampNow.isNone() || lampNow.isDying()) {
                openShop(ShopDressConfig.TYPE_OIL)
            } else {
                openDressPage(ShopDressConfig.TYPE_LAMP)
            }
        }
    }

    /** 静态素材：Glide 加载全画布透明 PNG；url 为空隐藏。返回是否可见 */
    private fun bindSceneImage(view: ImageView, url: String?): Boolean {
        if (url == null) {
            view.setImageDrawable(null)
            view.visibility = View.GONE
            return false
        }
        view.visibility = View.VISIBLE
        Glide.with(view).load(url).dontAnimate().into(view)
        return true
    }

    /** 动画素材：远程 Lottie 无限循环；url 为空隐藏。返回是否可见 */
    private fun bindSceneLottie(view: LottieAnimationView, url: String?): Boolean {
        val target = url?.takeIf { it.isNotBlank() }
        if (target == null) {
            view.tag = null
            view.cancelAnimation()
            view.visibility = View.GONE
            return false
        }
        if (view.tag == target && view.isAnimating) return true
        view.tag = target
        view.cancelAnimation()
        view.visibility = View.VISIBLE
        LottieCompositionFactory.fromUrl(requireContext(), target)
            .addListener({ composition ->
                if (isAdded && view.tag == target) {
                    //页面已回收或复用去加载别的素材就丢弃这次回调，防串台
                    view.setComposition(composition)
                    view.repeatCount = LottieDrawable.INFINITE
                    view.playAnimation()
                }
            })
            .addFailureListener { e ->
                log(TAG, "sceneAnim load fail: ${e.message}")
                if (view.tag == target) view.visibility = View.GONE
            }
        return true
    }

    /** 某品类当前佩戴的装扮配置（佩戴位指向的 id 要在持有列表里才能查到配置） */
    private fun wornConfig(dressType: Int): com.fingertip.baselib.bean.MyDressItem? {
        val data = myDressData ?: return null
        val id = data.wearing?.idOf(dressType) ?: 0
        if (id <= 0) return null
        return data.dresses.orEmpty().firstOrNull { it.dressId == id }
    }

    /**
     * 装扮点击分流：已拥有该品类任意一件 → 我的装扮（相当于背包，切换佩戴）；
     * 一件都没有 → 商城对应分类直接买。
     */
    private fun openDressPage(dressType: Int) {
        val owned = myDressData?.dresses.orEmpty().any { it.dressType == dressType }
        if (owned) {
            (parentFragment as? MainFragment)?.start(MyDressFragment())
        } else {
            openShop(dressType)
        }
    }

    /**
     * 商城入口：带品类跳过去。
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
                dressVM.loadMyDress()
            } else {
                lampData = null
                myDressData = null
                refreshScene()
            }
        }

        //灯状态：驱动场景里的佛灯/灯焰渲染；失败按"无灯"渲染，不在主视觉上摆错误提示
        lampVM.statusResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            lampData = if (result.success) result.data else null
            refreshScene()
        }

        //我的装扮：佩戴位变化（买了新龛/灯并佩戴）后场景要跟着换
        dressVM.myResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                myDressData = result.data
                refreshScene()
            }
        }

        //供奉结果：成功关弹窗并整刷佛像列表（香烛/贡品档次随供佛状态变化）
        mViewModel.worshipResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.worship_offer_success))
                worshipDialog?.onWorshipSuccess()
                mViewModel.initBuddhaList()
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.worship_offer_fail))
                worshipDialog?.onWorshipFailed()
            }
        }
    }

    /** 佩戴/灯状态/供佛档次任一变化，重新绑定可见页（页数少，notifyDataSetChanged 足够） */
    private fun refreshScene() {
        if (::pagerAdapter.isInitialized) pagerAdapter.notifyDataSetChanged()
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
            tv.setTextColor(TopApplication.instance.getColor(BaseR.color.c_gold))
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
        //每次回到这一页都刷一次灯状态和装扮：用户可能刚在商城买了灯油/换了佩戴；
        //多佛像后按当前佛像查，切佛像时也会各自刷
        if (GlobalConfig.session.isNotBlank()) {
            lampVM.loadStatus(currentBuddha?.id ?: 0)
            dressVM.loadMyDress()
        }
    }

    override fun onDestroyView() {
        binding.vpBuddha.unregisterOnPageChangeCallback(onPageChangeCallback)
        super.onDestroyView()
    }
}

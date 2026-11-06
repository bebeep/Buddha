package com.fingertip.uilib.fragment

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.HomeBlockConfig
import com.fingertip.baselib.bean.HomeSummaryData
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.HomeSection
import com.fingertip.uilib.adapter.HomeSectionAdapter
import com.fingertip.uilib.databinding.FragmentHomeBinding
import com.fingertip.uilib.dialog.QuoteCardDialog
import com.fingertip.uilib.fragment.book.BuddhaTextsFragment
import com.fingertip.uilib.fragment.game.FangShengFragment
import com.fingertip.uilib.fragment.moment.MomentDetailsFragment
import com.fingertip.uilib.fragment.practice.EarlyPracticeFragment
import com.fingertip.uilib.fragment.practice.MeditationFragment
import com.fingertip.uilib.fragment.temple.TempleListFragment
import com.fingertip.uilib.fragment.wishing.WishTreeFragment
import com.fingertip.uilib.fragment.worshiping.SignInFragment
import com.fingertip.uilib.viewmodel.HomeVM
import com.weikaiyun.fragmentation.SupportFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 首页（海外版重写）
 *
 * 六区块单 RecyclerView：
 *   1 佛历头卡（整卡进签到日历）
 *   2 今日修持（开始早课 + 诵经/抄经/签到三格）
 *   3 供灯状态条（三态，点击切拜佛 tab）
 *   4 金刚区（佛经/许愿/寺庙/念佛）
 *   5 游戏卡（禅院/放生/功德币商店）
 *   6 佛友圈精选（官方开示/社区热帖进详情，禅语日签弹日签卡）
 *
 * 排序逻辑沿用方案 10.2：日活锚（1-2）→ 转化（3）→ 导航（4）→ 时长（5-6），
 * 顺序和开关都读服务端 blocks，拿不到就用默认六区块。
 * 旧的 banner / 跑马灯 / 金刚区 GridView 全部下线。
 */
class HostFragment : TopVMFragment<HomeVM>() {

    companion object {
        private const val TAG = "HostFragment"

        //底部 tab 下标，与 MainFragment.fragmentList 顺序一致：首页 0 / 消息 1 / 拜佛 2 / 佛友圈 3 / 我的 4
        const val TAB_WORSHIP = 2
        const val TAB_MOMENT = 3

        //服务端没下发区块配置时的默认顺序
        private val DEFAULT_SECTIONS = listOf(
            HomeSectionAdapter.TYPE_BUDDHA_CARD,
            HomeSectionAdapter.TYPE_PRACTICE,
            HomeSectionAdapter.TYPE_LAMP,
            HomeSectionAdapter.TYPE_KINGKONG,
            HomeSectionAdapter.TYPE_GAME,
            HomeSectionAdapter.TYPE_MOMENT
        )
    }

    override fun layoutId() = R.layout.fragment_home

    override fun initVM() = HomeVM()

    private val binding get() = mBinding as FragmentHomeBinding

    private lateinit var adapter: HomeSectionAdapter

    //本次已请求过佛历的日期，跨天才重拉（佛历一天只变一次）
    private var buddhaLoadedDate: String? = null

    //首屏数据在 initShiTu 里拉过了，第一次 onVisible 不重复请求
    private var skipFirstVisible = true

    override fun initShiTu() {
        initRecycler()
        initRefresh()
        showSkeleton()
        loadAll()
    }

    private fun initRecycler() {
        adapter = HomeSectionAdapter(requireContext()).apply {
            onBuddhaCardClick = { openPage(SignInFragment()) }
            onStartPractice = { openPage(EarlyPracticeFragment()) }
            onPracticeSlotClick = { slot ->
                when (slot) {
                    //诵经：进佛经（书架/佛经双 tab，自带返回）
                    0 -> openPage(BuddhaTextsFragment())
                    //抄经必须带 bookId（抄哪一部），首页拿不到具体书目，
                    //所以先进佛经列表让用户选，选完在详情页进抄经
                    1 -> openPage(BuddhaTextsFragment())
                    //签到
                    else -> openPage(SignInFragment())
                }
            }
            onLampClick = { switchTab(TAB_WORSHIP) }
            onKingKongClick = { index ->
                when (index) {
                    0 -> openPage(BuddhaTextsFragment())
                    1 -> openPage(WishTreeFragment())
                    2 -> openPage(TempleListFragment())
                    else -> openPage(MeditationFragment())
                }
            }
            onGameClick = { index ->
                when (index) {
                    //禅院还没上线，卡片置灰，点了给提示
                    0 -> ToastUtil.showMessage(getString(R.string.home_game_coming))
                    //放生是现成的 H5 游戏
                    1 -> openPage(FangShengFragment())
                    //功德币商店要等商城上线
                    else -> ToastUtil.showMessage(getString(R.string.home_coming_soon))
                }
            }
            onMomentMoreClick = { switchTab(TAB_MOMENT) }
            //精选卡：有真实动态就进详情；禅语兜底卡没有 momentId，弹日签详情
            onMomentClick = { entity ->
                openPage(MomentDetailsFragment.newInstance(entity.momentId, entity))
            }
            onQuoteClick = { quote ->
                QuoteCardDialog(requireContext()).apply {
                    render(quote)
                    onGoMoment = { switchTab(TAB_MOMENT) }
                }.show()
            }
        }

        binding.rvContent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContent.adapter = adapter
        //区块数量固定，关掉条目动画，避免刷新时整屏闪一下
        binding.rvContent.itemAnimator = null
    }

    private fun initRefresh() {
        //刷新头用 UIApplication 里注册的全局默认（MaterialHeader），这里只关掉上拉加载。
        //用 OnRefreshLoadMoreListener 收口，和项目里其它列表保持一致（直接 setOnRefreshListener 有重载歧义）
        binding.srl.setEnableLoadMore(false)
        binding.srl.setOnRefreshLoadMoreListener(
            object : com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener {
                override fun onRefresh(refreshLayout: com.scwang.smart.refresh.layout.api.RefreshLayout) {
                    loadAll()
                }

                override fun onLoadMore(refreshLayout: com.scwang.smart.refresh.layout.api.RefreshLayout) {
                    //整页刷新不带上拉加载
                }
            })
    }

    /**
     * 首屏先只放骨架：聚合接口回来之前不铺假数据，避免骨架和真数据高度不一致地跳
     */
    private fun showSkeleton() {
        adapter.summary = null
        adapter.today = null
        adapter.initData(listOf(HomeSection(HomeSectionAdapter.TYPE_SKELETON)))
    }

    /**
     * 拉首页数据：佛历文案一天一次，聚合数据每次进页面都刷（签到/早课回来要更新）
     */
    private fun loadAll() {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        if (buddhaLoadedDate != date) {
            buddhaLoadedDate = date
            mViewModel.loadTodayBuddhaInfo()
        }

        //游客态没有 session，聚合接口必然回 INVALID_SESSION，直接按「空数据 + 默认六区块」渲染，
        //不然首屏只剩一条重试提示（海外版允许先逛后登录）
        if (GlobalConfig.session.isBlank()) {
            renderSummary(null)
            return
        }
        mViewModel.loadHomeSummary()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.summaryResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            binding.srl.finishRefresh()
            if (result.success) {
                binding.llError.visibility = View.GONE
                renderSummary(result.data)
            } else {
                log(TAG, "首页聚合失败：${result.errorCode} ${result.errorMsg}")
                //只有还没内容时才挡一屏错误，已有数据就保留旧内容静默失败
                if (isSkeletonShowing()) {
                    binding.llError.visibility = View.VISIBLE
                }
            }
        }

        mViewModel.todayBuddhaResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            //失败保持本地公历兜底，不提示
            if (result.success) {
                adapter.today = result.data
            }
        }
    }

    private fun renderSummary(data: HomeSummaryData?) {
        adapter.summary = data
        adapter.initData(buildSections(data?.blocks))
    }

    /**
     * 按服务端 blocks 拼区块：只留上线且认识的 key，按 sortOrder 升序；
     * 全部下线或没配置时退回默认六区块，保证首页不会开天窗
     */
    private fun buildSections(blocks: List<HomeBlockConfig>?): List<HomeSection> {
        val fromServer = blocks.orEmpty()
            .filter { it.isOnline() }
            .sortedBy { it.sortOrder }
            .map { HomeSectionAdapter.typeOf(it.blockKey) }
            .filter { it != 0 }
            .distinct()
            .map { HomeSection(it) }

        if (fromServer.isNotEmpty()) return fromServer
        return DEFAULT_SECTIONS.map { HomeSection(it) }
    }

    private fun isSkeletonShowing(): Boolean {
        val list = adapter.data()
        return list.isEmpty() || list.all { it.type == HomeSectionAdapter.TYPE_SKELETON }
    }

    /**
     * 打开二级页。首页是 MainFragment 的子页面，入口统一由宿主页 start
     */
    private fun openPage(fragment: SupportFragment) {
        (parentFragment as? MainFragment)?.start(fragment)
    }

    /**
     * 切底部 tab。
     *
     * 走 MainFragment 暴露的 selectTab：内部调用 BottomMenu.selectTab(index)，
     * 这样选中态、页面切换、lastIndex 三处状态都由 MainFragment 统一维护。
     * 不直接 findViewById（本工程约定：控件一律走 ViewBinding/宿主公开方法）。
     */
    private fun switchTab(index: Int) {
        (parentFragment as? MainFragment)?.selectTab(index)
    }

    override fun getClickViews() = listOf(binding.tvRetry)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.tvRetry -> {
                binding.llError.visibility = View.GONE
                showSkeleton()
                loadAll()
            }
        }
    }

    override fun onVisible() {
        super.onVisible()
        if (skipFirstVisible) {
            skipFirstVisible = false
            return
        }
        //从签到/早课等页面回来，静默刷一次首屏（不弹 loading，不重建列表）
        loadAll()
    }
}

package com.fingertip.uilib.fragment.shop

import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.fingertip.baselib.bean.MyDressItem
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentMyDressBinding
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.ShopVM

/**
 * 我的装扮（ViewPager 宿主）：已拥有的装扮管理，佩戴/卸下。
 *
 * 与商城同一套分类口径（灯具 / 佛龛 / 头像框 / 昵称 3D）；四个分类页
 * [MyDressTabFragment] 各自过滤持有列表、各自渲染空态，宿主只持有 tab、ViewPager
 * 与佩戴请求，成功后扇出刷新，保证四页的「佩戴中」标记一致。
 *
 * 锁定态：佛灯弹窗的「切换灯具」传 lockTab=true，ViewPager 禁止手势翻页，
 * 非灯具 tab 置灰且不可点击；个人中心/商城入口仍是普通构造，4 个 tab 照常可切。
 */
class MyDressFragment : TopPmFragment<ShopVM>() {

    companion object {
        private const val TAG = "MyDressFragment"
        private const val ARG_TYPE = "dressType"
        private const val ARG_LOCK_TAB = "lockTab"

        private val TAB_TYPES = listOf(
            ShopDressConfig.TYPE_LAMP,
            ShopDressConfig.TYPE_SHRINE,
            ShopDressConfig.TYPE_FRAME,
            ShopDressConfig.TYPE_NAME3D
        )

        /**
         * @param dressType 打开时定位的分类；佛灯弹窗的「切换灯具」传 TYPE_LAMP
         * @param lockTab true=只允许停留在 dressType 那一页（佛灯弹窗入口专用）
         */
        fun newInstance(
            dressType: Int = ShopDressConfig.TYPE_LAMP,
            lockTab: Boolean = false
        ): MyDressFragment = MyDressFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TYPE, dressType)
                putBoolean(ARG_LOCK_TAB, lockTab)
            }
        }
    }

    override fun layoutId(): Int = R.layout.fragment_my_dress

    override fun initVM(): ShopVM = provideVM()

    private val binding get() = mBinding as FragmentMyDressBinding

    private lateinit var tabFragments: List<MyDressTabFragment>

    private var currentType: Int = ShopDressConfig.TYPE_LAMP

    /** 外部明确指定分类时不自动跳到「第一个有物品的分类」，佛灯弹窗进灯具页不能被拐走 */
    private var explicitStartType = false
    private var lockTab = false
    private var lockedType: Int = ShopDressConfig.TYPE_LAMP

    /** 首次进页面是否已经自动定位到有物品的分类（只做一次，之后尊重用户选择） */
    private var autoSelected = false

    /** 佩戴结果回来时判断该弹「佩戴成功」还是「卸下成功」 */
    private var pendingWasWearing = false

    /** 点 tab / 左右滑都只改 currentType；锁定时把意外滑走的页面拉回来 */
    private val pageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            val type = TAB_TYPES.getOrElse(position) { ShopDressConfig.TYPE_LAMP }
            currentType = type
            if (lockTab && type != lockedType) {
                binding.vpMyDress.setCurrentItem(TAB_TYPES.indexOf(lockedType), false)
            }
        }
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        if (v == binding.ivBack) pop()
    }

    /**
     * 去商城。从商城进来的就退回商城（顺便切到当前分类），
     * 别再压一层新的商城页，否则「商城 → 我的装扮 → 商城 → …」会无限叠栈。
     */
    fun goShop() {
        val shop = parentFragment as? ShopFragment
        if (shop != null) {
            shop.switchCategory(currentType)
            pop()
        } else {
            openPage(ShopFragment.newInstance(currentType))
        }
    }

    override fun initShiTu() {
        explicitStartType = arguments?.containsKey(ARG_TYPE) == true
        lockTab = arguments?.getBoolean(ARG_LOCK_TAB, false) ?: false
        val arg = arguments?.getInt(ARG_TYPE, ShopDressConfig.TYPE_LAMP)
            ?: ShopDressConfig.TYPE_LAMP
        lockedType = if (arg in TAB_TYPES) arg else ShopDressConfig.TYPE_LAMP
        currentType = lockedType

        tabFragments = TAB_TYPES.map { MyDressTabFragment.newInstance(it) }
        binding.vpMyDress.offscreenPageLimit = TAB_TYPES.size - 1
        binding.vpMyDress.adapter = TopFragmentPagerAdapter(tabFragments, childFragmentManager)
        val titles = arrayOf(
            getString(R.string.shop_cat_lamp),
            getString(R.string.shop_cat_shrine),
            getString(R.string.shop_cat_frame),
            getString(R.string.shop_cat_name3d)
        )
        binding.tabLayout.normalTextSize = 13f
        binding.tabLayout.selectTextSize = 14f
        binding.tabLayout.setViewPager(binding.vpMyDress, titles)
        binding.vpMyDress.addOnPageChangeListener(pageChangeListener)

        val startIndex = TAB_TYPES.indexOf(lockedType).coerceAtLeast(0)
        if (lockTab) {
            //锁定态：先定位到灯具页，再禁手势、置灰其余 tab
            binding.vpMyDress.setCurrentItem(startIndex, false)
            binding.vpMyDress.swipeEnabled = false
            applyLockedTabs(startIndex)
        } else {
            //普通入口：显式传了分类就落到那一页，否则从灯具页开始
            binding.vpMyDress.setCurrentItem(if (explicitStartType) startIndex else 0, false)
        }

        //宿主只需要一份持有态做「自动跳到第一个有物品的分类」判断；
        //各分类页自己拉自己的数据，互不复用这份结果
        mViewModel.loadMyDress(showLoading = true)
    }

    /**
     * 锁定态下把非目标 tab 置灰并禁用点击。
     * ScaleTabLayout 的 click listener 挂在 tab 外层容器上，所以同时禁用标题及其父容器。
     */
    private fun applyLockedTabs(lockedIndex: Int) {
        for (i in TAB_TYPES.indices) {
            val title = binding.tabLayout.getTitleView(i) ?: continue
            val tabRoot = title.parent as? View ?: title
            val enabled = !lockTab || i == lockedIndex
            tabRoot.isEnabled = enabled
            tabRoot.isClickable = enabled
            tabRoot.alpha = if (enabled) 1f else 0.4f
        }
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.myResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (!result.success) {
                log(TAG, "我的装扮失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.shop_load_fail))
                return@observe
            }
            if (lockTab || autoSelected || explicitStartType) return@observe
            val list = result.data?.dresses.orEmpty()
            if (list.isEmpty()) return@observe
            val target = list.map { it.dressType }
                .filter { it in TAB_TYPES }
                .minOrNull() ?: return@observe
            autoSelected = true
            if (target != currentType) {
                currentType = target
                binding.vpMyDress.setCurrentItem(TAB_TYPES.indexOf(target), true)
            }
        }

        mViewModel.wearResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                ToastUtil.showMessage(
                    getString(if (pendingWasWearing) R.string.shop_take_off_success else R.string.shop_wear_success)
                )
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.shop_load_fail))
            }
            //以服务端状态为准刷新所有分类页：佩戴位是每类一个，其它页的按钮也要跟着变
            refreshTabs()
        }
    }

    private fun refreshTabs() {
        if (!isAdded || !::tabFragments.isInitialized) return
        tabFragments.forEach { it.refresh() }
    }

    /** 供子分类页调用：佩戴/卸下由宿主统一发请求 */
    fun toggleWear(item: MyDressItem, wasWearing: Boolean) {
        pendingWasWearing = wasWearing
        mViewModel.wear(item.dressId, !wasWearing)
    }

    override fun onDestroyView() {
        if (::tabFragments.isInitialized) {
            binding.vpMyDress.removeOnPageChangeListener(pageChangeListener)
        }
        super.onDestroyView()
    }
}

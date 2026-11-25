package com.fingertip.uilib.fragment.shop

import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.activity.LoginRegisterActivity
import com.fingertip.uilib.databinding.FragmentShopBinding
import com.fingertip.uilib.dialog.ConfirmDialog
import com.fingertip.uilib.dialog.PayConfirmDialog
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.ShopVM

/**
 * 装扮商店（ViewPager 宿主）
 *
 * 四类永久装扮：灯具 / 佛龛 / 头像框 / 昵称 3D，买完可以直接佩戴。
 * 灯油是消耗品，已从商店 tab 移除，改由拜佛页佛灯弹窗「点灯/续燃」直接购买。
 *
 * 页面结构：宿主只持有标题栏、tab、ViewPager 与购买链路；四个分类页
 * [ShopTabFragment] 各自持有 VM / 列表 / 空态，点 tab 与左右滑都只改 ViewPager 的
 * currentItem，因此不存在单 RecyclerView 方案里「旧请求盖到新分类」的串页问题。
 * 购买、佩戴这两类写操作保留在宿主，成功后统一通知各页刷新。
 *
 * 购买链路（v2.20 起全站统一功德币，不再走 IAP）：
 *   确认弹窗 → dress/buyByCoin（服务端扣币+发货，返回余额）→ 回写余额 → 刷新持有列表 → 自动佩戴。
 */
class ShopFragment : TopPmFragment<ShopVM>() {

    companion object {
        private const val TAG = "ShopFragment"
        private const val ARG_TYPE = "dressType"
        private const val ARG_BUDDHA = "buddhaId"

        /** tab 顺序与 ShopDressConfig.TYPE_* 一一对应 */
        private val TAB_TYPES = listOf(
            ShopDressConfig.TYPE_LAMP,
            ShopDressConfig.TYPE_SHRINE,
            ShopDressConfig.TYPE_FRAME,
            ShopDressConfig.TYPE_NAME3D
        )

        /**
         * @param dressType 打开时默认选中的品类；灯油不再有商店 tab
         * @param buddhaId 预留参数：点灯/续燃已在拜佛页弹窗内完成，不再经商店传佛像
         */
        fun newInstance(
            dressType: Int = ShopDressConfig.TYPE_LAMP,
            buddhaId: Int = 0
        ): ShopFragment = ShopFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TYPE, dressType)
                putInt(ARG_BUDDHA, buddhaId)
            }
        }
    }

    override fun layoutId(): Int = R.layout.fragment_shop

    override fun initVM(): ShopVM = provideVM()

    private val binding get() = mBinding as FragmentShopBinding

    private lateinit var tabFragments: List<ShopTabFragment>

    /** 当前分类，供「我的装扮 → 去商城」回跳时定位 */
    private var currentType: Int = ShopDressConfig.TYPE_LAMP

    //等待结果的商品，购买回调是异步的，只能先存着
    private var pendingDress: ShopDressConfig? = null

    //预留：点灯/续燃不再经过商店，买其他装扮时按 0 处理
    private var targetBuddhaId = 0

    /** tab 与 ViewPager 共用一份状态：page 变化只更新 currentType，不重复拉列表 */
    private val pageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            currentType = TAB_TYPES.getOrElse(position) { ShopDressConfig.TYPE_LAMP }
        }
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack, binding.tvMyDress)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvMyDress -> openMyDress()
        }
    }

    override fun initShiTu() {
        targetBuddhaId = arguments?.getInt(ARG_BUDDHA, 0) ?: 0
        val arg = arguments?.getInt(ARG_TYPE, ShopDressConfig.TYPE_LAMP)
            ?: ShopDressConfig.TYPE_LAMP
        //传进来的品类不合法就退回灯具，避免入口参数写错导致空白页
        currentType = if (arg in TAB_TYPES) arg else ShopDressConfig.TYPE_LAMP

        tabFragments = TAB_TYPES.map { ShopTabFragment.newInstance(it) }
        binding.vpShop.offscreenPageLimit = TAB_TYPES.size - 1
        binding.vpShop.adapter = TopFragmentPagerAdapter(tabFragments, childFragmentManager)
        val titles = arrayOf(
            getString(R.string.shop_cat_lamp),
            getString(R.string.shop_cat_shrine),
            getString(R.string.shop_cat_frame),
            getString(R.string.shop_cat_name3d)
        )
        binding.tabLayout.normalTextSize = 13f
        binding.tabLayout.selectTextSize = 14f
        binding.tabLayout.setViewPager(binding.vpShop, titles)
        binding.vpShop.addOnPageChangeListener(pageChangeListener)
        binding.vpShop.setCurrentItem(TAB_TYPES.indexOf(currentType), false)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.buyResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val dress = pendingDress
            pendingDress = null
            if (!result.success) {
                //余额不足/重复购买这类都是正常业务码，服务端已给出可读文案
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.shop_pay_fail))
                return@observe
            }
            //服务端返回扣费后的余额：直接回写全局用户信息，商城/我的页立刻同步
            result.data?.toIntOrNull()?.let { coin -> GlobalConfig.userData?.coin = coin }
            if (dress == null) return@observe
            if (dress.isOil()) {
                //灯油：服务端发货时已经点燃了灯，这里只提示去拜佛页看
                ToastUtil.showMessage(getString(R.string.shop_pay_success_oil))
                refreshTabs()
            } else {
                //永久装扮：到账后自动佩戴，省掉用户再点一次
                ToastUtil.showMessage(getString(R.string.shop_pay_success))
                mViewModel.wear(dress.id, true)
            }
        }

        mViewModel.wearResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.shop_wear_success))
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.shop_load_fail))
            }
            //无论成败都以服务端状态为准刷新所有分类页（含刚买的那一类）
            refreshTabs()
        }
    }

    /** 各分类页自己重拉，写操作只在宿主发生，这里做一次扇出即可 */
    private fun refreshTabs() {
        if (!isAdded || !::tabFragments.isInitialized) return
        tabFragments.forEach { it.refresh() }
    }

    /**
     * 供子分类页调用：商品卡点击。油直接买；永久装扮按「已拥有 → 佩戴」「未拥有 → 购买」分流。
     * owned/wearing 由点击发生的那一页传入——持有态是本页独立渲染的，宿主不缓存副本，
     * 避免四页状态同步时拿到别的分类的旧数据。
     */
    fun onDressClick(item: ShopDressConfig, owned: Boolean, wearing: Boolean) {
        if (item.isOil()) {
            confirmPurchase(item)
            return
        }
        if (wearing) {
            ToastUtil.showMessage(getString(R.string.shop_action_in_use))
            return
        }
        if (owned) {
            mViewModel.wear(item.id, true)
            return
        }
        confirmPurchase(item)
    }

    private fun confirmPurchase(item: ShopDressConfig) {
        if (GlobalConfig.session.isBlank()) {
            ToastUtil.showMessage(getString(R.string.shop_login_first))
            return
        }
        //游客号花钱前先提醒一次：不绑定的话卸载重装就找不回了。
        //取消键 = 知道风险仍要继续购买，必须真的继续，否则游客永远买不了（此前就断在这）
        if (GlobalConfig.isGuestAccount) {
            ConfirmDialog(
                requireContext(),
                title = getString(R.string.shop_guest_bind_title),
                message = getString(R.string.shop_guest_bind_msg),
                confirmText = getString(R.string.shop_guest_bind_go),
                cancelText = getString(R.string.shop_guest_bind_continue),
                onCancel = { showPayConfirm(item) },
                onConfirm = {
                    //清栈重进登录页：绑定成功后和冷启动登录一样回到干净的主页栈
                    startActivity(
                        android.content.Intent(requireContext(), LoginRegisterActivity::class.java).apply {
                            addFlags(
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            )
                        }
                    )
                }
            ).show()
            return
        }
        showPayConfirm(item)
    }

    private fun showPayConfirm(item: ShopDressConfig) {
        PayConfirmDialog(requireContext(), item) {
            pendingDress = item
            mViewModel.buyByCoin(item.id, targetBuddhaId)
        }.show()
    }

    private fun openMyDress() {
        //不能强转 parentFragment：商城也可能是别的页面里再打开的，
        //那样转成 MainFragment 会得到 null，点了毫无反应（就是之前这个 bug）
        openPage(MyDressFragment())
    }

    /**
     * 外部（我的装扮「去商城」）把页面切到指定品类。
     * 只切 ViewPager 坐标，不额外压栈、不重复拉列表。
     */
    fun switchCategory(type: Int) {
        if (!isAdded || !::tabFragments.isInitialized || type !in TAB_TYPES) return
        currentType = type
        binding.vpShop.setCurrentItem(TAB_TYPES.indexOf(type), true)
    }

    override fun onDestroyView() {
        if (::tabFragments.isInitialized) {
            binding.vpShop.removeOnPageChangeListener(pageChangeListener)
        }
        super.onDestroyView()
    }
}

package com.fingertip.uilib.fragment.shop

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.MyDressData
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.activity.LoginRegisterActivity
import com.fingertip.uilib.adapter.ShopDressAdapter
import com.fingertip.uilib.databinding.FragmentShopBinding
import com.fingertip.uilib.dialog.ConfirmDialog
import com.fingertip.uilib.dialog.PayConfirmDialog
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.ShopVM

/**
 * 装扮商店
 *
 * 五类商品：灯具 / 佛龛 / 头像框 / 昵称 3D / 灯油。
 * 前四类是永久买断，买完可以直接佩戴；灯油是消耗品，买一次点一次灯，没有拥有态。
 *
 * 购买链路（v2.20 起全站统一功德币，不再走 IAP）：
 *   确认弹窗 → dress/buyByCoin（服务端扣币+发货，返回余额）→ 回写余额 → 刷新持有列表 → 自动佩戴
 * 一步到位，没有「钱扣了没发货」的中间态。
 *
 * 游客可以逛（列表接口公开），点购买时提示登录。
 */
class ShopFragment : TopPmFragment<ShopVM>() {

    companion object {
        private const val TAG = "ShopFragment"
        private const val ARG_TYPE = "dressType"
        private const val ARG_BUDDHA = "buddhaId"

        /**
         * @param dressType 打开时默认选中的品类，见 [ShopDressConfig.TYPE_LAMP] 等
         * @param buddhaId 拜佛页带过来的目标佛像id：买灯油时建单落到该佛像，发货即点亮在其佛前。
         *                 其它入口（我的页/我的装扮）不传，按 0 处理（灯油订单不指定佛像）
         */
        fun newInstance(dressType: Int = ShopDressConfig.TYPE_LAMP, buddhaId: Int = 0): ShopFragment {
            val f = ShopFragment()
            f.arguments = Bundle().apply {
                putInt(ARG_TYPE, dressType)
                putInt(ARG_BUDDHA, buddhaId)
            }
            return f
        }
    }

    override fun layoutId(): Int = R.layout.fragment_shop

    override fun initVM(): ShopVM = provideVM()

    private val binding get() = mBinding as FragmentShopBinding

    private lateinit var adapter: ShopDressAdapter

    //分类 chip 按品类下标排列（0 位不用）
    private val chips by lazy {
        listOf(binding.tvCat1, binding.tvCat2, binding.tvCat3, binding.tvCat4, binding.tvCat5)
    }

    private var currentType = ShopDressConfig.TYPE_LAMP

    //我的装扮缓存：用来判断每个商品是「购买 / 佩戴 / 使用中」
    private var myData: MyDressData? = null

    //等待结果的商品，购买回调是异步的，只能先存着
    private var pendingDress: ShopDressConfig? = null

    //买灯油时的目标佛像id（拜佛页入口带入），建单随单落库
    private var targetBuddhaId = 0

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
        val arg = arguments?.getInt(ARG_TYPE, ShopDressConfig.TYPE_LAMP) ?: ShopDressConfig.TYPE_LAMP
        //传进来的品类不合法就退回灯具，避免入口参数写错导致空白页
        currentType = if (arg in ShopDressConfig.TYPE_LAMP..ShopDressConfig.TYPE_OIL) {
            arg
        } else {
            ShopDressConfig.TYPE_LAMP
        }

        initRecycler()
        initChips()
        selectChip(currentType)
        mViewModel.loadMyDress()
    }

    private fun initRecycler() {
        adapter = ShopDressAdapter(requireContext()) { item -> onDressClick(item) }
        binding.rcList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rcList.adapter = adapter
        //商品数量少，关掉条目动画避免切分类时整屏闪
        binding.rcList.itemAnimator = null
    }

    private fun initChips() {
        chips.forEachIndexed { index, chip ->
            chip.setOnClickListener { selectChip(index + 1) }
        }
    }

    /**
     * 切换品类：换选中样式 + 拉对应分类的商品
     */
    private fun selectChip(type: Int) {
        currentType = type
        chips.forEachIndexed { index, chip ->
            val selected = index + 1 == type
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_shop_chip_on else R.drawable.bg_home_pill_solid
            )
            chip.setTextColor(
                requireContext().getColor(
                    if (selected) R.color.shop_text_on_gold else R.color.shop_text_chip_off
                )
            )
        }
        mViewModel.loadList(type)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.listResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                val list = result.data.orEmpty()
                adapter.initData(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            } else {
                log(TAG, "商品列表失败：${result.errorCode} ${result.errorMsg}")
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }

        mViewModel.myResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                renderMyDress(result.data)
            } else {
                //未登录就是 INVALID_SESSION，游客态不需要提示，商品照常可逛
                log(TAG, "我的装扮失败：${result.errorCode} ${result.errorMsg}")
            }
        }

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
            result.data?.toIntOrNull()?.let { coin ->
                GlobalConfig.userData?.coin = coin
            }
            if (dress == null) return@observe
            if (dress.isOil()) {
                //灯油：服务端发货时已经点燃了灯，这里只提示去拜佛页看
                ToastUtil.showMessage(getString(R.string.shop_pay_success_oil))
                mViewModel.loadList(currentType)
                mViewModel.loadMyDress()
            } else {
                //永久装扮：到账后自动佩戴，省掉用户再点一次
                ToastUtil.showMessage(getString(R.string.shop_pay_success))
                mViewModel.wear(dress.id, true)
                mViewModel.loadList(currentType)
            }
        }

        mViewModel.wearResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.shop_wear_success))
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.shop_load_fail))
            }
            //无论成败都以服务端状态为准重新拉一次，避免本地状态和服务端不一致
            mViewModel.loadMyDress()
        }
    }

    private fun renderMyDress(data: MyDressData?) {
        myData = data
        adapter.ownedIds = data?.dresses.orEmpty().map { it.dressId }.toSet()
        //佩戴中的 id：四类各一个槽位，都算「使用中」
        val wearing = data?.wearing
        adapter.wearingIds = if (wearing == null) emptySet() else setOf(
            wearing.lampDressId,
            wearing.shrineDressId,
            wearing.frameDressId,
            wearing.name3dDressId
        ).filter { it > 0 }.toSet()
        adapter.notifyDataSetChanged()
    }

    /**
     * 点商品卡：油直接买；永久装扮按「已拥有 → 佩戴」「未拥有 → 购买」分流
     */
    private fun onDressClick(item: ShopDressConfig) {
        if (item.isOil()) {
            confirmPurchase(item)
            return
        }
        val wearing = item.id in adapter.wearingIds
        if (wearing) {
            //已经在用了，不弹任何东西，避免误触后一脸问号
            ToastUtil.showMessage(getString(R.string.shop_action_in_use))
            return
        }
        if (item.id in adapter.ownedIds) {
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
     * 外部（我的装扮「去商城」）把分类切到指定品类。
     * 只切 UI 与列表，不额外压栈。
     */
    fun switchCategory(type: Int) {
        if (type in ShopDressConfig.TYPE_LAMP..ShopDressConfig.TYPE_OIL) {
            selectChip(type)
        }
    }
}

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
import com.fingertip.uilib.pay.IapBridge
import com.fingertip.uilib.viewmodel.ShopVM

/**
 * 装扮商店
 *
 * 五类商品：灯具 / 佛龛 / 头像框 / 昵称 3D / 灯油。
 * 前四类是永久买断，买完可以直接佩戴；灯油是消耗品，买一次点一次灯，没有拥有态。
 *
 * 购买链路（顺序不能变）：
 *   建内部订单 → 拉起商店支付 → 验单发货 → 刷新持有列表 → 自动佩戴
 * 先建单再支付，是为了让服务端在验单时能对上「谁买什么、多少钱」；
 * 反过来做一旦客户端崩溃就会变成「钱扣了没发货」的客诉。
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

    //等待支付回执的商品与订单号，支付回调是异步的，只能先存着
    private var pendingDress: ShopDressConfig? = null
    private var pendingOrderNo: String? = null

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
                if (selected) 0xFF14100C.toInt() else 0xFFB5A78F.toInt()
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

        mViewModel.orderResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                val orderNo = result.data
                val dress = pendingDress
                if (orderNo.isNullOrBlank() || dress == null) return@observe
                pendingOrderNo = orderNo
                launchPay(dress, orderNo)
            } else {
                pendingDress = null
                pendingOrderNo = null
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.shop_pay_fail))
            }
        }

        mViewModel.verifyResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val dress = pendingDress
            if (result.success) {
                pendingDress = null
                pendingOrderNo = null
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
            } else {
                //订单已建但验单失败：服务端保留了订单记录，属于可追查状态，提示联系客服
                log(TAG, "验单失败：${result.errorCode} ${result.errorMsg} orderNo=$pendingOrderNo")
                pendingDress = null
                pendingOrderNo = null
                ToastUtil.showMessage(getString(R.string.shop_verify_fail))
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
        //游客号花钱前先提醒一次：不绑定的话卸载重装就找不回了。取消键继续买，不打断
        if (GlobalConfig.isGuestAccount) {
            ConfirmDialog(
                requireContext(),
                title = getString(R.string.shop_guest_bind_title),
                message = getString(R.string.shop_guest_bind_msg),
                confirmText = getString(R.string.shop_guest_bind_go),
                cancelText = getString(R.string.shop_guest_bind_continue)
            ) {
                //清栈重进登录页：绑定成功后和冷启动登录一样回到干净的主页栈
                startActivity(
                    android.content.Intent(requireContext(), LoginRegisterActivity::class.java).apply {
                        addFlags(
                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )
                    }
                )
            }.show()
            return
        }
        PayConfirmDialog(requireContext(), item) {
            pendingDress = item
            mViewModel.createOrder(item.id, targetBuddhaId)
        }.show()
    }

    /**
     * 建单成功后拉起商店支付。支付回执回来再走验单，验单才算发货。
     */
    private fun launchPay(dress: ShopDressConfig, orderNo: String) {
        // 灯油是消耗型：买完要 consume，否则同一件商品第二次买不了
        val consumable = dress.dressType == ShopDressConfig.TYPE_OIL
        IapBridge.launchPurchase(
            activity = activity,
            productId = dress.androidProductId(),
            accountId = GlobalConfig.userData?.accountId ?: 0,
            consumable = consumable
        ) { result ->
            if (!isAdded) return@launchPurchase
            val payOrderId = result.payOrderId
            if (result.success && !payOrderId.isNullOrBlank()) {
                mViewModel.verify(orderNo, payOrderId)
            } else {
                //用户取消或通道未接入：订单留着不动，服务端没发货，不存在资损
                log(TAG, "支付未完成：${result.message} orderNo=$orderNo")
                pendingDress = null
                pendingOrderNo = null
                ToastUtil.showMessage(result.message ?: getString(R.string.shop_pay_fail))
            }
        }
    }

    private fun openMyDress() {
        (parentFragment as? com.fingertip.uilib.fragment.MainFragment)?.start(MyDressFragment())
    }
}

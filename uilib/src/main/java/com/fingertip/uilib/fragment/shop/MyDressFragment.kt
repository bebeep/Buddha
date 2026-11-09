package com.fingertip.uilib.fragment.shop

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.MyDressData
import com.fingertip.baselib.bean.MyDressItem
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MyDressAdapter
import com.fingertip.uilib.databinding.FragmentMyDressBinding
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.ShopVM
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R as BaseR

/**
 * 我的装扮：已拥有的装扮管理，佩戴/卸下。
 *
 * 与装扮商城同一套分类口径（灯具 / 佛龛 / 头像框 / 昵称 3D）和同一套 2 列卡片网格，
 * 差别只在数据源：商城主页拉 dress/getList，这里拉 dress/getMy 后按 dressType 本地过滤。
 * 灯油是消耗品没有持有态，所以分类里不列灯油。
 *
 * 佩戴状态存在服务端（user_dress_wearing 一人一行，四类各一个槽位），
 * 拜佛页场景按 resKey 换模型，所以这里切完立即生效，不需要额外同步。
 */
class MyDressFragment : TopPmFragment<ShopVM>() {

    companion object {
        private const val TAG = "MyDressFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_my_dress

    override fun initVM(): ShopVM = provideVM()

    private val binding get() = mBinding as FragmentMyDressBinding

    private lateinit var adapter: MyDressAdapter

    //分类 chip 按品类下标排列（1..4 对应灯具/佛龛/头像框/昵称3D）
    private val chips by lazy {
        listOf(binding.tvCat1, binding.tvCat2, binding.tvCat3, binding.tvCat4)
    }

    private var currentType = ShopDressConfig.TYPE_LAMP

    //接口返回的全部持有项，切分类只做本地过滤，不重复请求
    private var allDresses: List<MyDressItem> = emptyList()

    //当前佩戴中的装扮 id
    private var wearingIds: Set<Int> = emptySet()

    //首次进页面是否已经自动定位到有物品的分类（只做一次，之后尊重用户选择）
    private var autoSelected = false

    //当前正在处理的那一件，用于佩戴成功后判断提示文案
    private var pendingItem: MyDressItem? = null

    override fun getClickViews(): List<View> = listOf(binding.ivBack, binding.tvGoShop)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvGoShop -> goShop()
        }
    }

    /**
     * 去商城。从商城进来的就退回商城（顺便切到当前分类），
     * 别再压一层新的商城页，否则「商城 → 我的装扮 → 商城 → …」会无限叠栈。
     */
    private fun goShop() {
        val shop = parentFragment as? ShopFragment
        if (shop != null) {
            shop.switchCategory(currentType)
            pop()
        } else {
            openPage(ShopFragment.newInstance(currentType))
        }
    }

    override fun initShiTu() {
        adapter = MyDressAdapter(requireContext()) { item -> toggleWear(item) }
        //本页空态有"全空 vs 分类空"两种文案，保留页面自己的占位
        adapter.showEmptyTip = false
        binding.rcList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rcList.adapter = adapter
        //持有量少，关掉条目动画避免切分类时整屏闪
        binding.rcList.itemAnimator = null
        initChips()
        selectChip(ShopDressConfig.TYPE_LAMP)
        mViewModel.loadMyDress(showLoading = true)
    }

    private fun initChips() {
        chips.forEachIndexed { index, chip ->
            chip.setOnClickListener { selectChip(index + 1) }
        }
    }

    /**
     * 切换分类：只换选中样式 + 本地过滤，不发请求
     */
    private fun selectChip(type: Int) {
        currentType = type
        chips.forEachIndexed { index, chip ->
            val selected = index + 1 == type
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_shop_chip_on else R.drawable.bg_home_pill_solid
            )
            chip.setTextColor(
                if (selected) {
                    context?.resources?.getColor(R.color.shop_text_on_gold) ?: TopApplication.instance.getColor(BaseR.color.c_text_on_gold)
                } else {
                    context?.resources?.getColor(R.color.shop_text_chip_off) ?: TopApplication.instance.getColor(BaseR.color.c_text_third)
                }
            )
        }
        renderList()
    }

    /**
     * 按当前分类渲染。空态分两种说法：一件都没有 vs 只有这个分类没有
     */
    private fun renderList() {
        val list = allDresses.filter { it.dressType == currentType }
        adapter.wearingIds = wearingIds
        adapter.initData(list)

        val empty = list.isEmpty()
        binding.llEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rcList.visibility = if (empty) View.GONE else View.VISIBLE
        binding.tvEmptyTip.setText(
            if (allDresses.isEmpty()) R.string.shop_my_dress_empty else R.string.shop_my_dress_empty_cat
        )
    }

    /**
     * 拿到持有数据：先缓存全量，首次进来主动跳到第一个有物品的分类，
     * 免得用户明明买了头像框却先看到空空的灯具页
     */
    private fun applyData(data: MyDressData?) {
        val list = data?.dresses.orEmpty()
        val wearing = data?.wearing
        wearingIds = if (wearing == null) {
            emptySet()
        } else {
            listOfNotNull(
                wearing.lampDressId,
                wearing.shrineDressId,
                wearing.frameDressId,
                wearing.name3dDressId
            ).filter { it > 0 }.toSet()
        }
        allDresses = list

        if (!autoSelected && list.isNotEmpty()) {
            autoSelected = true
            val target = list.map { it.dressType }
                .filter { it in ShopDressConfig.TYPE_LAMP..ShopDressConfig.TYPE_NAME3D }
                .minOrNull()
            if (target != null && list.none { it.dressType == currentType }) {
                //selectChip 内部会 renderList，这里直接返回避免重复渲染
                selectChip(target)
                return
            }
        }
        renderList()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.myResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                applyData(result.data)
            } else {
                log(TAG, "我的装扮失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.shop_load_fail))
            }
        }

        mViewModel.wearResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val wasWearing = pendingItem?.let { it.dressId in wearingIds } ?: false
            pendingItem = null
            if (result.success) {
                ToastUtil.showMessage(
                    getString(if (wasWearing) R.string.shop_take_off_success else R.string.shop_wear_success)
                )
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.shop_load_fail))
            }
            //以服务端状态为准重新拉一次
            mViewModel.loadMyDress()
        }
    }

    private fun toggleWear(item: MyDressItem) {
        pendingItem = item
        val wearing = item.dressId in wearingIds
        mViewModel.wear(item.dressId, !wearing)
    }
}

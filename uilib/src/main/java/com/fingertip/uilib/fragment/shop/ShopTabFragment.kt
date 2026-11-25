package com.fingertip.uilib.fragment.shop

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.MyDressData
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.ShopDressAdapter
import com.fingertip.uilib.databinding.FragShopTabBinding
import com.fingertip.uilib.viewmodel.ShopVM

/**
 * 装扮商城的单个分类页。
 *
 * 每一页持有自己的 ShopVM（ViewModelProvider(this) 按 Fragment 隔离）、自己的商品列表
 * 与自己的空态，左右滑动只切页面，不会出现「 A 分类的请求回来盖到 B 分类」的串页。
 * 购买 / 佩戴这类写操作仍收口在宿主 [ShopFragment]：购买成功由宿主通知本页刷新，
 * 避免同一笔购买在四页各弹一次确认框。
 */
class ShopTabFragment : TopPmFragment<ShopVM>() {

    companion object {
        private const val TAG = "ShopTabFragment"
        private const val ARG_TYPE = "dressType"

        fun newInstance(type: Int): ShopTabFragment = ShopTabFragment().apply {
            arguments = Bundle().apply { putInt(ARG_TYPE, type) }
        }
    }

    override fun layoutId(): Int = R.layout.frag_shop_tab

    override fun initVM(): ShopVM = provideVM()

    private val binding get() = mBinding as FragShopTabBinding

    private var dressType: Int = ShopDressConfig.TYPE_LAMP
    private lateinit var adapter: ShopDressAdapter

    /** ViewPager 的子 Fragment，parentFragment 就是宿主商城页 */
    private fun host(): ShopFragment? = parentFragment as? ShopFragment

    override fun initShiTu() {
        dressType = arguments?.getInt(ARG_TYPE, ShopDressConfig.TYPE_LAMP)
            ?: ShopDressConfig.TYPE_LAMP
        adapter = ShopDressAdapter(requireContext()) { item ->
            //持有/佩戴判断用本页自己的 adapter 状态，不跨页取，避免切 tab 后串数据
            host()?.onDressClick(
                item,
                owned = item.id in adapter.ownedIds,
                wearing = item.id in adapter.wearingIds
            )
        }
        adapter.emptyText = getString(R.string.shop_empty)
        binding.rcList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rcList.adapter = adapter
        //商品数量少，关掉条目动画避免切分类时整屏闪
        binding.rcList.itemAnimator = null
        mViewModel.loadList(dressType)
        mViewModel.loadMyDress()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.listResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                adapter.initData(result.data.orEmpty())
            } else {
                log(TAG, "商品列表失败：${result.errorCode} ${result.errorMsg}")
                adapter.showEmptyNow()
            }
        }

        mViewModel.myResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                applyMyDress(result.data)
            } else {
                //游客态没有持有数据，商品照常可逛
                log(TAG, "我的装扮失败：${result.errorCode} ${result.errorMsg}")
            }
        }
    }

    /** 商品卡上的「购买 / 佩戴 / 使用中」状态来自宿主持有态 */
    private fun applyMyDress(data: MyDressData?) {
        adapter.ownedIds = data?.dresses.orEmpty().map { it.dressId }.toSet()
        val wearing = data?.wearing
        adapter.wearingIds = if (wearing == null) emptySet() else setOf(
            wearing.lampDressId,
            wearing.shrineDressId,
            wearing.frameDressId,
            wearing.name3dDressId
        ).filter { it > 0 }.toSet()
        adapter.notifyDataSetChanged()
    }

    /** 宿主购买/佩戴成功后调用：本页独立重拉，不影响其他分类页 */
    fun refresh() {
        if (!isAdded || view == null) return
        mViewModel.loadList(dressType)
        mViewModel.loadMyDress()
    }
}

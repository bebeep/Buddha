package com.fingertip.uilib.fragment.shop

import android.os.Bundle
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
import com.fingertip.uilib.databinding.FragMyDressTabBinding
import com.fingertip.uilib.viewmodel.ShopVM

/**
 * 我的装扮的单个分类页：本地按 dressType 过滤持有列表，四类各自一份列表、空态与佩戴状态。
 *
 * 数据拉取与佩戴动作和商城一样收口在宿主：本页只负责渲染 + 把点击交给宿主，
 * 宿主佩戴成功后调 [refresh] 统一刷新所有分类页，避免某页还挂着旧的「佩戴中」。
 */
class MyDressTabFragment : TopPmFragment<ShopVM>() {

    companion object {
        private const val TAG = "MyDressTabFragment"
        private const val ARG_TYPE = "dressType"

        fun newInstance(type: Int): MyDressTabFragment = MyDressTabFragment().apply {
            arguments = Bundle().apply { putInt(ARG_TYPE, type) }
        }
    }

    override fun layoutId(): Int = R.layout.frag_my_dress_tab

    override fun initVM(): ShopVM = provideVM()

    private val binding get() = mBinding as FragMyDressTabBinding

    private var dressType: Int = ShopDressConfig.TYPE_LAMP
    private lateinit var adapter: MyDressAdapter

    private var allDresses: List<MyDressItem> = emptyList()
    private var wearingIds: Set<Int> = emptySet()

    /** ViewPager 的子 Fragment，parentFragment 就是宿主「我的装扮」页 */
    private fun host(): MyDressFragment? = parentFragment as? MyDressFragment

    override fun initShiTu() {
        dressType = arguments?.getInt(ARG_TYPE, ShopDressConfig.TYPE_LAMP)
            ?: ShopDressConfig.TYPE_LAMP
        adapter = MyDressAdapter(requireContext()) { item ->
            host()?.toggleWear(item, item.dressId in wearingIds)
        }
        //本页空态有「全空 vs 本分类空」两种文案，保留页面自己的占位
        adapter.showEmptyTip = false
        binding.rcList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rcList.adapter = adapter
        binding.rcList.itemAnimator = null
        binding.tvGoShop.setOnClickListener { host()?.goShop() }
        mViewModel.loadMyDress(showLoading = true)
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
    }

    /** 缓存全量持有项，本页只展示自己那一类 */
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
        renderList()
    }

    private fun renderList() {
        val list = allDresses.filter { it.dressType == dressType }
        adapter.wearingIds = wearingIds
        adapter.initData(list)

        val empty = list.isEmpty()
        binding.llEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rcList.visibility = if (empty) View.GONE else View.VISIBLE
        binding.tvEmptyTip.setText(
            if (allDresses.isEmpty()) R.string.shop_my_dress_empty else R.string.shop_my_dress_empty_cat
        )
    }

    /** 宿主佩戴/卸下成功后调用：本页独立重拉持有态 */
    fun refresh() {
        if (!isAdded || view == null) return
        mViewModel.loadMyDress()
    }
}

package com.fingertip.uilib.fragment.shop

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.MyDressItem
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MyDressAdapter
import com.fingertip.uilib.databinding.FragmentMyDressBinding
import com.fingertip.uilib.viewmodel.ShopVM

/**
 * 我的装扮：已拥有的装扮管理，佩戴/卸下。
 *
 * 佩戴状态存在服务端（user_dress_wearing 一人一行，四类各一个槽位），
 * 拜佛页 3D 场景按 resKey 换模型，所以这里切完立即生效，不需要额外同步。
 */
class MyDressFragment : TopPmFragment<ShopVM>() {

    companion object {
        private const val TAG = "MyDressFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_my_dress

    override fun initVM(): ShopVM = provideVM()

    private val binding get() = mBinding as FragmentMyDressBinding

    private lateinit var adapter: MyDressAdapter

    //当前正在处理的那一件，用于佩戴成功后判断提示文案
    private var pendingItem: MyDressItem? = null

    override fun getClickViews(): List<View> = listOf(binding.ivBack, binding.tvGoShop)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvGoShop -> (parentFragment as? com.fingertip.uilib.fragment.MainFragment)
                ?.start(ShopFragment.newInstance())
        }
    }

    override fun initShiTu() {
        adapter = MyDressAdapter(requireContext()) { item -> toggleWear(item) }
        binding.rcList.layoutManager = LinearLayoutManager(requireContext())
        binding.rcList.adapter = adapter
        mViewModel.loadMyDress(showLoading = true)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.myResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                val data = result.data
                val list = data?.dresses.orEmpty()
                adapter.wearingIds = listOfNotNull(
                    data?.wearing?.lampDressId,
                    data?.wearing?.shrineDressId,
                    data?.wearing?.frameDressId,
                    data?.wearing?.name3dDressId
                ).filter { it > 0 }.toSet()
                adapter.initData(list)

                val empty = list.isEmpty()
                binding.llEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                binding.rcList.visibility = if (empty) View.GONE else View.VISIBLE
            } else {
                log(TAG, "我的装扮失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.shop_load_fail))
            }
        }

        mViewModel.wearResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val wasWearing = pendingItem?.let { it.dressId in adapter.wearingIds } ?: false
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
        val wearing = item.dressId in adapter.wearingIds
        mViewModel.wear(item.dressId, !wearing)
    }
}

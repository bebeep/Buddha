package com.fingertip.uilib.fragment.wishing

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MyWishAdapter
import com.fingertip.uilib.databinding.FragmentMyWishBinding
import com.fingertip.uilib.dialog.BlessListDialog
import com.fingertip.uilib.viewmodel.WishVM

/**
 * 我的愿望页，对应原型「许愿-我的愿望」
 * 点整条看收到的祝福，点删除删掉这条愿望
 */
class MyWishFragment : TopPmFragment<WishVM>() {

    companion object {
        private const val TAG = "MyWishFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_my_wish

    override fun initVM(): WishVM = provideVM()

    private val binding get() = mBinding as FragmentMyWishBinding

    private lateinit var adapter: MyWishAdapter

    private var blessListDialog: BlessListDialog? = null

    //当前祝福弹窗对应的愿望 id，翻页时要用
    private var showingWishingId = 0

    //翻页中标记，防止滚动手抖重复请求
    private var loadingMore = false

    override fun initShiTu() {
        adapter = MyWishAdapter(
            requireContext(),
            onShowBless = { wish -> showBlessList(wish.id) },
            onDelete = { wish ->
                mViewModel.deleteWishing(wish.id)
            }
        )
        binding.rcMyWish.layoutManager = LinearLayoutManager(requireContext())
        binding.rcMyWish.adapter = adapter
        mViewModel.loadMyWishes(0)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.myWishResult.observe(viewLifecycleOwner) { result ->
            loadingMore = false
            if (result.success) {
                adapter.initData(result.data)
                binding.tvEmpty.visibility =
                    if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            } else {
                log(TAG, "我的愿望获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败，请稍后再试")
            }
        }

        mViewModel.deleteWishResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                ToastUtil.showMessage("已删除")
                mViewModel.loadMyWishes(0)
            } else {
                ToastUtil.showMessage(result.errorMsg ?: "删除失败，请稍后再试")
            }
        }

        mViewModel.wishBlessListResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                blessListDialog?.render(result.data, append = loadingMore)
            } else {
                blessListDialog?.render(emptyList(), append = false)
                ToastUtil.showMessage(result.errorMsg ?: "祝福加载失败，请稍后再试")
            }
            loadingMore = false
        }
    }

    /**
     * 看某条愿望收到的祝福
     */
    private fun showBlessList(wishingId: Int) {
        showingWishingId = wishingId
        val dialog = blessListDialog ?: BlessListDialog(requireContext()).also {
            it.onLoadMore = { pageNum ->
                loadingMore = true
                mViewModel.loadWishBlessList(showingWishingId, pageNum)
            }
            blessListDialog = it
        }
        dialog.show()
        mViewModel.loadWishBlessList(wishingId, 0)
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        if (v == binding.ivBack) pop()
    }
}

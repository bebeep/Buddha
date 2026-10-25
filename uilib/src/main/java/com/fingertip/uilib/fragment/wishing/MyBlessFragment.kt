package com.fingertip.uilib.fragment.wishing

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.BlessRecordAdapter
import com.fingertip.uilib.databinding.FragmentMyBlessBinding
import com.fingertip.uilib.viewmodel.WishVM

/**
 * 我的祝愿页，对应原型「许愿-我的祝愿」：我送出去的祝福记录
 */
class MyBlessFragment : TopPmFragment<WishVM>() {

    companion object {
        private const val TAG = "MyBlessFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_my_bless

    override fun initVM(): WishVM = provideVM()

    private val binding get() = mBinding as FragmentMyBlessBinding

    private lateinit var adapter: BlessRecordAdapter

    private var pageNum = 0
    private var hasMore = false
    private var loadingMore = false

    override fun initShiTu() {
        adapter = BlessRecordAdapter(requireContext())
        binding.rcMyBless.layoutManager = LinearLayoutManager(requireContext())
        binding.rcMyBless.adapter = adapter
        mViewModel.loadMyBless(0)

        //滚到底部自动翻页
        binding.rcMyBless.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0 || loadingMore || !hasMore) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val last = lm.findLastVisibleItemPosition()
                if (last >= adapter.itemCount - 3) {
                    loadingMore = true
                    mViewModel.loadMyBless(pageNum + 1)
                }
            }
        })
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.myBlessResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                val append = loadingMore
                val data = result.data.orEmpty()
                if (append) {
                    adapter.addData(data)
                    pageNum++
                } else {
                    adapter.initData(data)
                    pageNum = 0
                }
                hasMore = data.size >= WishVM.PAGE_SIZE
                binding.tvEmpty.visibility =
                    if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            } else {
                log(TAG, "我的祝愿获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败，请稍后再试")
            }
            loadingMore = false
        }
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        if (v == binding.ivBack) pop()
    }
}

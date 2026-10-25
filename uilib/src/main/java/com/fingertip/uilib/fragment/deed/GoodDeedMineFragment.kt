package com.fingertip.uilib.fragment.deed

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.DeedBean
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.DeedListAdapter
import com.fingertip.uilib.databinding.FragmentDeedMineBinding
import com.fingertip.uilib.viewmodel.GoodDeedVM

/**
 * 我的行善：页签切换「我发布的求助」与「善行记录（我捐赠的）」
 */
class GoodDeedMineFragment : TopPmFragment<GoodDeedVM>() {

    companion object {
        private const val TAG = "GoodDeedMineFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_deed_mine

    override fun initVM(): GoodDeedVM = provideVM()

    private val binding get() = mBinding as FragmentDeedMineBinding

    private lateinit var adapter: DeedListAdapter

    //0 我发布的 1 我捐赠的
    private var tab = 0
    private var pageNum = 0

    override fun initShiTu() {
        adapter = DeedListAdapter(requireContext()) { deed ->
            (parentFragment as? com.fingertip.uilib.fragment.MainFragment)?.start(
                GoodDeedDetailFragment.newInstance(deed.id)
            )
        }
        binding.rcList.layoutManager = LinearLayoutManager(requireContext())
        binding.rcList.adapter = adapter

        binding.rcList.addOnScrollListener(object :
            androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && lm.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    load(tab, pageNum + 1)
                }
            }
        })

        load(0, 0)
    }

    private fun load(tabIndex: Int, page: Int) {
        tab = tabIndex
        pageNum = page
        if (tab == 0) mViewModel.loadMyDeeds(page) else mViewModel.loadMyDonateDeeds(page)
    }

    override fun initObserver() {
        super.initObserver()

        val observer = { result: com.fingertip.baselib.viewmodel.RequestResult<List<DeedBean>> ->
            if (result.success) {
                val list = result.data.orEmpty()
                if (pageNum == 0) adapter.initData(list) else adapter.addData(list)
                binding.tvEmpty.visibility =
                    if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            } else {
                log(TAG, "我的行善获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败")
            }
        }

        mViewModel.myDeedResult.observe(viewLifecycleOwner) { observer(it) }
        mViewModel.myDonateResult.observe(viewLifecycleOwner) { observer(it) }
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack, binding.tvTabPosted, binding.tvTabDonated
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvTabPosted -> {
                if (tab == 0) return
                binding.tvTabPosted.setBackgroundResource(R.drawable.bg_praying_cover_selected)
                binding.tvTabPosted.setTextColor(Color.parseColor("#C87A2B"))
                binding.tvTabDonated.setBackgroundResource(R.drawable.bg_praying_cover_normal)
                binding.tvTabDonated.setTextColor(Color.parseColor("#8A6B3A"))
                load(0, 0)
            }
            binding.tvTabDonated -> {
                if (tab == 1) return
                binding.tvTabDonated.setBackgroundResource(R.drawable.bg_praying_cover_selected)
                binding.tvTabDonated.setTextColor(Color.parseColor("#C87A2B"))
                binding.tvTabPosted.setBackgroundResource(R.drawable.bg_praying_cover_normal)
                binding.tvTabPosted.setTextColor(Color.parseColor("#8A6B3A"))
                load(1, 0)
            }
        }
    }
}

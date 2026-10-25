package com.fingertip.uilib.fragment.blessing

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MyPrayingAdapter
import com.fingertip.uilib.databinding.FragmentPrayingMineBinding
import com.fingertip.uilib.viewmodel.BlessingVM

/**
 * 我的祈福 / 查看所有祈福列表页
 * isAll=true 时拉 getAllPrayingRecord，否则拉我的祈福（可删除自己的）
 */
class PrayingMineFragment : TopPmFragment<BlessingVM>() {

    companion object {
        private const val ARG_IS_ALL = "arg_is_all"
        private const val TAG = "PrayingMineFragment"

        fun newInstance(isAll: Boolean): PrayingMineFragment {
            return PrayingMineFragment().apply {
                arguments = Bundle().apply { putBoolean(ARG_IS_ALL, isAll) }
            }
        }
    }

    override fun layoutId(): Int = R.layout.fragment_praying_mine

    override fun initVM(): BlessingVM = provideVM()

    private val binding get() = mBinding as FragmentPrayingMineBinding

    private lateinit var adapter: MyPrayingAdapter
    private var isAll = false
    private var pageNum = 0

    override fun initShiTu() {
        isAll = arguments?.getBoolean(ARG_IS_ALL, false) ?: false
        binding.tvTitle.text = if (isAll) "所有祈福" else "我的祈福"

        adapter = MyPrayingAdapter(requireContext()) { praying ->
            if (isAll) return@MyPrayingAdapter
            //删除直接确认，dev阶段用toast确认
            ToastUtil.showMessage("再次点击删除该福卡")
            if (pendingDeleteId == praying.id) {
                mViewModel.deletePraying(praying.id)
            } else {
                pendingDeleteId = praying.id
            }
        }
        binding.rcList.layoutManager = LinearLayoutManager(requireContext())
        binding.rcList.adapter = adapter

        binding.rcList.addOnScrollListener(object :
            androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && lm.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    load(pageNum + 1)
                }
            }
        })

        load(0)
    }

    private var pendingDeleteId = -1

    private fun load(page: Int) {
        pageNum = page
        if (isAll) mViewModel.loadAllPraying(page) else mViewModel.loadMyPraying(page)
    }

    override fun initObserver() {
        super.initObserver()

        val observer = { result: com.fingertip.baselib.viewmodel.RequestResult<List<com.fingertip.baselib.bean.PrayingBean>> ->
            if (result.success) {
                val list = result.data.orEmpty()
                if (pageNum == 0) adapter.initData(list) else adapter.addData(list)
                binding.tvEmpty.visibility =
                    if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            } else {
                log(TAG, "祈福列表获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败，请稍后再试")
            }
        }

        mViewModel.myPrayingResult.observe(viewLifecycleOwner) { observer(it) }
        mViewModel.allPrayingResult.observe(viewLifecycleOwner) { observer(it) }

        mViewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                ToastUtil.showMessage("已删除")
                pendingDeleteId = -1
                load(0)
            } else {
                ToastUtil.showMessage(result.errorMsg ?: "删除失败")
            }
        }
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
        }
    }
}

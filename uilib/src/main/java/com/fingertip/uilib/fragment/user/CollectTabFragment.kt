package com.fingertip.uilib.fragment.user

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.CollectData
import com.fingertip.baselib.event_bus.EventBusProxy
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CollectAdapter
import com.fingertip.uilib.databinding.FragCollectTabBinding
import com.fingertip.uilib.fragment.book.FojingDetailsFragment
import com.fingertip.uilib.fragment.moment.MomentDetailsFragment
import com.fingertip.uilib.fragment.temple.TempleDetailFragment
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.UserToolVM
import org.greenrobot.eventbus.Subscribe

/**
 * 我的收藏的单个分页：全部 / 佛经 / 感悟 / 古刹。
 *
 * 每种类型各自持有分页状态，ViewPager 左右滑过来不会被别的 tab 冲掉；
 * 取消收藏发事件让所有已加载的 tab 同步摘条目，避免「全部」里还挂着已取消的项。
 */
class CollectTabFragment : TopPmFragment<UserToolVM>() {

    companion object {
        const val ARG_TYPE = "arg_type"
        /** 服务端每页 20 条，与 GlobalConfig.PAGE_SIZE 对齐 */
        private const val PAGE_SIZE = 20

        fun newInstance(type: Int): CollectTabFragment = CollectTabFragment().apply {
            arguments = Bundle().apply { putInt(ARG_TYPE, type) }
        }
    }

    override fun layoutId() = R.layout.frag_collect_tab
    override fun initVM(): UserToolVM = provideVM()

    private val binding get() = mBinding as FragCollectTabBinding

    private var targetType = CollectData.TYPE_ALL
    private var pageNum = 0
    private var hasMore = true
    private var loading = false
    private var pendingCancel: CollectData? = null
    private lateinit var adapter: CollectAdapter

    override fun initShiTu() {
        targetType = arguments?.getInt(ARG_TYPE, CollectData.TYPE_ALL) ?: CollectData.TYPE_ALL
        adapter = CollectAdapter(requireContext(),
            onClick = { openDetail(it) },
            onCancel = {
                pendingCancel = it
                mViewModel.cancelCollect(it.targetType, it.targetId)
            }
        )
        adapter.emptyText = getString(R.string.collect_empty)

        val manager = LinearLayoutManager(requireContext())
        binding.rvCollect.layoutManager = manager
        binding.rvCollect.adapter = adapter
        binding.rvCollect.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || loading || !hasMore) return
                //最后一条可见就继续拉，收藏量一般不大，不做预加载
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - 2) loadPage()
            }
        })

        loadPage()
    }

    override fun initObserver() {
        super.initObserver()
        mViewModel.collectListResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            loading = false
            if (result.success) {
                val list = result.data.orEmpty()
                hasMore = list.size >= PAGE_SIZE
                if (pageNum == 0) adapter.initData(list) else adapter.addData(list)
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.collect_cancel_fail))
            }
        }

        mViewModel.cancelCollectResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val target = pendingCancel
            pendingCancel = null
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.collect_cancel_done))
                if (target != null) {
                    EventBusProxy.post(MessageEvent(EventConstant.EVENT_COLLECT_CANCEL, target))
                }
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.collect_cancel_fail))
            }
        }
    }

    /** 任一 tab 取消收藏：本页已加载的列表也把那条摘掉 */
    @Subscribe
    fun onCollectCancel(event: MessageEvent) {
        if (!isAdded || event.what != EventConstant.EVENT_COLLECT_CANCEL) return
        val target = event.`object` as? CollectData ?: return
        val index = adapter.mlist.indexOfFirst {
            it.targetType == target.targetType && it.targetId == target.targetId
        }
        if (index >= 0) {
            adapter.mlist.removeAt(index)
            adapter.notifyItemRemoved(index)
        }
    }

    private fun loadPage() {
        loading = true
        mViewModel.loadCollectList(targetType, pageNum)
        pageNum++
    }

    private fun openDetail(item: CollectData) {
        when (item.targetType) {
            CollectData.TYPE_SUTRA -> openPage(FojingDetailsFragment.newInstance(item.targetId))
            CollectData.TYPE_MOMENT -> openPage(MomentDetailsFragment.newInstance(item.targetId))
            CollectData.TYPE_TEMPLE -> openPage(TempleDetailFragment.newInstance(item.targetId))
        }
    }
}

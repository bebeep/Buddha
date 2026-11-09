package com.fingertip.uilib.fragment.user

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.CollectData
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CollectAdapter
import com.fingertip.uilib.databinding.FragmentCollectBinding
import com.fingertip.uilib.fragment.book.FojingDetailsFragment
import com.fingertip.uilib.fragment.moment.MomentDetailsFragment
import com.fingertip.uilib.fragment.temple.TempleDetailFragment
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.UserToolVM
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R as BaseR

/**
 * 我的收藏
 *
 * 三类目标（佛经 / 感悟 / 寺庙）共用一个列表，chip 做类型过滤。
 * 点击条目按类型跳对应详情页——收藏的价值就是"再打开一次"，
 * 如果只能看不能进，这页就没有存在意义。
 */
class CollectListFragment : TopPmFragment<UserToolVM>() {

    companion object {
        /** 服务端每页 20 条，与 GlobalConfig.PAGE_SIZE 对齐 */
        private const val PAGE_SIZE = 20
    }

    override fun layoutId() = R.layout.fragment_collect

    override fun initVM(): UserToolVM = provideVM()

    private val binding get() = mBinding as FragmentCollectBinding

    private lateinit var adapter: CollectAdapter

    private var currentType = CollectData.TYPE_ALL
    private var pageNum = 0
    private var hasMore = true
    private var loading = false

    //正在取消收藏的那条，回调成功后从列表摘掉
    private var pendingCancel: CollectData? = null

    override fun initShiTu() {
        adapter = CollectAdapter(requireContext(),
            onClick = { item -> openDetail(item) },
            onCancel = { item ->
                pendingCancel = item
                mViewModel.cancelCollect(item.targetType, item.targetId)
            }
        )
        adapter.emptyText = getString(R.string.collect_empty)

        val manager = LinearLayoutManager(requireContext())
        binding.rvCollect.layoutManager = manager
        binding.rvCollect.adapter = adapter

        binding.rvCollect.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || loading || !hasMore) return
                //最后一条可见就继续拉，收藏量一般不大，不做花哨的预加载
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - 2) {
                    loadPage()
                }
            }
        })

        switchType(CollectData.TYPE_ALL)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.collectListResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            loading = false
            if (result.success) {
                val list = result.data ?: emptyList()
                hasMore = list.size >= PAGE_SIZE
                if (pageNum == 0) adapter.initData(list) else adapter.addData(list)
                refreshEmptyState()
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.collect_cancel_fail))
                refreshEmptyState()
            }
        }

        mViewModel.cancelCollectResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val target = pendingCancel
            pendingCancel = null
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.collect_cancel_done))
                if (target != null) {
                    val index = adapter.mlist.indexOfFirst {
                        it.targetType == target.targetType && it.targetId == target.targetId
                    }
                    if (index != -1) {
                        adapter.mlist.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                }
                refreshEmptyState()
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.collect_cancel_fail))
            }
        }
    }

    private fun switchType(type: Int) {
        currentType = type
        pageNum = 0
        hasMore = true
        loading = false
        adapter.initData(emptyList())
        refreshTabs()
        loadPage()
    }

    private fun loadPage() {
        loading = true
        mViewModel.loadCollectList(currentType, pageNum)
        pageNum++
    }

    /**
     * chip 选中态：金色描边 + 亮金字，未选中弱化。
     * 直接改背景和字色，不额外造 selector，状态只有两种。
     */
    private fun refreshTabs() {
        val tabs = listOf(
            CollectData.TYPE_ALL to binding.tvTabAll,
            CollectData.TYPE_SUTRA to binding.tvTabSutra,
            CollectData.TYPE_MOMENT to binding.tvTabMoment,
            CollectData.TYPE_TEMPLE to binding.tvTabTemple
        )
        tabs.forEach { (type, tv) ->
            val on = type == currentType
            tv.setBackgroundResource(
                if (on) R.drawable.bg_collect_tab_on else R.drawable.bg_collect_tab_off
            )
            tv.setTextColor(if (on) TopApplication.instance.getColor(BaseR.color.c_gold_light) else TopApplication.instance.getColor(BaseR.color.c_text_second))
        }
    }

    private fun refreshEmptyState() {
        //空态占位由 TopRcAdapter 统一处理
    }

    private fun openDetail(item: CollectData) {
        when (item.targetType) {
            CollectData.TYPE_SUTRA -> openPage(FojingDetailsFragment.newInstance(item.targetId))
            CollectData.TYPE_MOMENT -> openPage(MomentDetailsFragment.newInstance(item.targetId))
            CollectData.TYPE_TEMPLE -> openPage(TempleDetailFragment.newInstance(item.targetId))
        }
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack,
        binding.tvTabAll,
        binding.tvTabSutra,
        binding.tvTabMoment,
        binding.tvTabTemple
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvTabAll -> if (currentType != CollectData.TYPE_ALL) switchType(CollectData.TYPE_ALL)
            binding.tvTabSutra -> if (currentType != CollectData.TYPE_SUTRA) switchType(CollectData.TYPE_SUTRA)
            binding.tvTabMoment -> if (currentType != CollectData.TYPE_MOMENT) switchType(CollectData.TYPE_MOMENT)
            binding.tvTabTemple -> if (currentType != CollectData.TYPE_TEMPLE) switchType(CollectData.TYPE_TEMPLE)
        }
    }
}

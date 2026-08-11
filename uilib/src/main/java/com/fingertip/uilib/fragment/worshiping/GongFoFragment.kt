package com.fingertip.uilib.fragment.worshiping

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.BuddhaBannerAdapter
import com.fingertip.uilib.adapter.BuddhaListAdapter
import com.fingertip.uilib.databinding.FragmentGfBinding
import com.fingertip.uilib.viewmodel.WorshipVM
import com.youth.banner.listener.OnPageChangeListener
import org.greenrobot.eventbus.Subscribe
import kotlin.math.abs

/**
 * 供佛
 *
 * 功能：
 * - 使用 globalParam.buddhaConfig 加载佛像列表
 * - buddhaConfig 为空时调用 ViewModel 接口获取
 * - Banner 与 RecyclerView 双向联动，滑动任一方同步到另一方
 */
class GongFoFragment : TopPmFragment<WorshipVM>() {

    companion object {
        private const val TAG = "GongFoFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_gf

    override fun initVM(): WorshipVM = provideVM()

    private val binding get() = mBinding as FragmentGfBinding

    private lateinit var adapter: BuddhaListAdapter
    private lateinit var bannerAdapter: BuddhaBannerAdapter

    /** 防止双向联动死循环标志 */
    private var isSyncing = false

    /** 当前选中的佛像列表数据 */
    private var buddhaList: List<BuddhaConfig> = emptyList()

    override fun initShiTu() {
        initBanner()
        initRecyclerView()
        loadBuddhaData()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.buddhaConfigResult.observe(viewLifecycleOwner) { result ->
            if (result.success && !result.data.isNullOrEmpty()) {
                buddhaList = result.data!!
                updateBuddhaList(buddhaList)
            } else {
                log(TAG, "获取佛像配置失败")
            }
        }
    }

    /**
     * 初始化 Banner：禁用自动滚动和指示器
     */
    private fun initBanner() {
        bannerAdapter = BuddhaBannerAdapter(requireContext(), emptyList())
        // setAdapter(adapter, false) 第二个参数 false 表示不支持无限循环
        binding.bannerFo.setAdapter(bannerAdapter, false)
        // 禁用自动轮播
        binding.bannerFo.isAutoLoop(false)

        // Banner 滑动联动 RecyclerView
        binding.bannerFo.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                if (isSyncing) return
                isSyncing = true
                adapter.setSelectedPosition(position)
                binding.rcFo.smoothScrollToPosition(position)
                isSyncing = false
            }
            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    /**
     * 初始化水平 RecyclerView
     */
    private fun initRecyclerView() {
        adapter = BuddhaListAdapter(requireContext()) { position ->
            // 点击选中某一项时，联动 Banner 滑动到对应位置
            if (isSyncing) return@BuddhaListAdapter
            isSyncing = true
            binding.bannerFo.setCurrentItem(position, false)
            isSyncing = false
        }
        binding.rcFo.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rcFo.adapter = adapter

        // 添加滑动监听：滑动时联动 Banner
        binding.rcFo.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
                    val centerPos = findCenterPosition()
                    if (centerPos >= 0) {
                        adapter.setSelectedPosition(centerPos)
                        syncBannerTo(centerPos)
                    }
                }
            }

            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val centerPos = findCenterPosition()
                if (centerPos >= 0) {
                    adapter.setSelectedPosition(centerPos)
                    syncBannerTo(centerPos)
                }
            }
        })
    }

    /**
     * 将 Banner 同步到指定位置（防止循环调用）
     */
    private fun syncBannerTo(position: Int) {
        if (isSyncing) return
        isSyncing = true
        binding.bannerFo.setCurrentItem(position, false)
        isSyncing = false
    }

    /**
     * 查找当前 RecyclerView 中最居中的 item 位置
     */
    private fun findCenterPosition(): Int {
        val recyclerView = binding.rcFo
        val centerX = recyclerView.width / 2
        var closestChild: View? = null
        var closestDistance = Int.MAX_VALUE

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val childCenter = (child.left + child.right) / 2
            val distance = abs(childCenter - centerX)
            if (distance < closestDistance) {
                closestDistance = distance
                closestChild = child
            }
        }

        return if (closestChild != null) {
            recyclerView.getChildAdapterPosition(closestChild)
        } else {
            -1
        }
    }

    /**
     * 加载佛像数据
     * 优先使用 globalParam.buddhaConfig，为空时调用接口获取
     */
    private fun loadBuddhaData() {
        val config = GlobalConfig.globalParam?.buddhaConfig
        if (!config.isNullOrEmpty()) {
            log(TAG, "使用 globalParam 中的佛像配置，共 ${config.size} 项")
            buddhaList = config
            updateBuddhaList(config)
        } else {
            log(TAG, "globalParam.buddhaConfig 为空，调用接口获取")
            mViewModel.getBuddhaConfig()
        }
    }

    /**
     * 更新佛像列表数据到 Adapter，并初始化 Banner
     */
    private fun updateBuddhaList(list: List<BuddhaConfig>) {
        adapter.initData(list)
        // 更新 Banner 数据
        val newBannerAdapter = BuddhaBannerAdapter(requireContext(), list)
        binding.bannerFo.setAdapter(newBannerAdapter, false)
        bannerAdapter = newBannerAdapter
        // 默认选中第一项
        if (list.isNotEmpty()) {
            adapter.setSelectedPosition(0)
            binding.bannerFo.setCurrentItem(0, false)
        }
    }

    override fun getClickViews(): List<View> {
        return listOf(binding.ivBack, binding.tvGf)
    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
    }

    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when (event.what) {

        }
    }
}

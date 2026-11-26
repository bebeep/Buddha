package com.fingertip.uilib.fragment.user

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FollowUserAdapter
import com.fingertip.uilib.databinding.FragFansFollowTabBinding
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.FansFollowVM
import org.greenrobot.eventbus.Subscribe

/**
 * 粉丝 / 关注的单个分页。
 *
 * 每页自己维护 pageNum / hasMore / loading，ViewPager 左右滑过来不串页；
 * 卡片用关系列表专用的 FollowUserAdapter（与搜索页解耦），点头像进对方主页。
 */
class FansFollowTabFragment : TopPmFragment<FansFollowVM>() {

    companion object {
        private const val ARG_TAB = "arg_tab"
        /** 与 GlobalConfig.PAGE_SIZE / 后端 GlobalConfig.PAGE_SIZE 对齐 */
        private const val PAGE_SIZE = 20

        fun newInstance(tab: Int): FansFollowTabFragment {
            return FansFollowTabFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TAB, tab)
                }
            }
        }
    }

    override fun layoutId(): Int = R.layout.frag_fans_follow_tab
    override fun initVM(): FansFollowVM = provideVM()

    private val binding get() = mBinding as FragFansFollowTabBinding

    //直接读 arguments：基类先调 initObserver() 再调 initShiTu()，
    //用 var 在 initShiTu 里赋值会导致订阅时 tab 还是默认粉丝，关注页收到粉丝数据
    private val tab: Int
        get() = arguments?.getInt(ARG_TAB, FansFollowListFragment.TAB_FANS)
            ?: FansFollowListFragment.TAB_FANS
    /** 下一页要请求的页码：只在请求成功后推进，失败留着供重试 */
    private var pageNum: Int = 0
    /** 本次在途请求对应的页码；loading 保证同一时刻只有一个在途请求，不会被覆盖 */
    private var pendingPage: Int = 0
    private var hasMore: Boolean = true
    private var loading: Boolean = false
    /** 有在途请求时收到刷新，先记下来，等响应处理完再重拉，避免两次请求结果互相覆盖 */
    private var pendingRefresh: Boolean = false
    private lateinit var adapter: FollowUserAdapter

    override fun initShiTu() {
        adapter = FollowUserAdapter(requireContext()) { entity ->
            openPage(UserInfoFragment.newInstance(entity.accountId))
        }
        adapter.emptyText = getString(
            if (tab == FansFollowListFragment.TAB_FANS) {
                R.string.fans_follow_fans_empty
            } else {
                R.string.fans_follow_following_empty
            }
        )

        val manager = LinearLayoutManager(requireContext())
        binding.rvUsers.layoutManager = manager
        binding.rvUsers.adapter = adapter
        binding.rvUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || loading || !hasMore) return
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - 2) loadPage()
            }
        })
        //空列表没有可滚动内容，点空态区域重试第 0 页（覆盖层挂在 root 上，点击会冒泡到这里）
        binding.root.setOnClickListener {
            if (!loading && adapter.itemCount == 0) {
                pageNum = 0
                hasMore = true
                loadPage()
            }
        }

        loadPage()
    }

    override fun initObserver() {
        super.initObserver()
        //本页只订阅与自己 tab 对应的 LiveData，避免另一 tab 的请求结果冲掉列表
        val liveData = if (tab == FansFollowListFragment.TAB_FANS) {
            mViewModel.fansResult
        } else {
            mViewModel.followingResult
        }
        liveData.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            //过期/重复响应保护：只有存在在途请求时才处理结果，
            //页码取发请求前记录的 pendingPage，而不是此刻已推进的 pageNum
            if (!loading) return@observe
            val page = pendingPage
            loading = false
            if (result.success) {
                val list = result.data.orEmpty()
                hasMore = list.size >= PAGE_SIZE
                //按本次请求页码分支：第 0 页整页替换，后续页追加
                if (page == 0) adapter.initData(list) else adapter.addData(list)
                //只有成功才推进页码；失败时保持原页码，滚动/点空态可重试当前页
                pageNum = page + 1
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.fans_follow_load_fail))
                if (page == 0) adapter.showEmptyNow()
            }
            //在途请求期间收到的刷新，等本次响应处理完再执行，避免两次请求结果互相覆盖
            if (pendingRefresh) {
                pendingRefresh = false
                requestRefresh()
            }
        }
    }

    /**
     * 从列表点头像进他人主页关注/取关后，返回时列表必须跟着变。
     * 只有「关注」tab 受"我关注谁"影响；粉丝 tab 是别人关注我，不随本端操作变化，不刷新。
     */
    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        if (event.what != EventConstant.EVENT_FOLLOW_CHANGED) return
        if (tab != FansFollowListFragment.TAB_FOLLOWING) return
        requestRefresh()
    }

    private fun requestRefresh() {
        //列表页在返回栈里视图通常还在；万一已销毁就别碰 binding，交给下次创建时首屏加载
        if (!isAdded || view == null) return
        if (loading) {
            pendingRefresh = true
            return
        }
        pageNum = 0
        hasMore = true
        loadPage()
    }

    private fun loadPage() {
        if (loading || !hasMore) return
        loading = true
        pendingPage = pageNum
        if (tab == FansFollowListFragment.TAB_FANS) {
            mViewModel.loadFans(pendingPage)
        } else {
            mViewModel.loadFollowing(pendingPage)
        }
    }
}

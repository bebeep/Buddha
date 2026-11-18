package com.fingertip.uilib.fragment.im

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.ConversationAdapter
import com.fingertip.uilib.databinding.FragmentConversationBinding
import com.fingertip.uilib.im.ConversationCache
import com.fingertip.uilib.im.ConversationItem
import com.fingertip.uilib.im.ImManager
import com.fingertip.uilib.im.ImUtil
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.MainVM
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMConversationResult
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMValueCallback
import org.greenrobot.eventbus.Subscribe

/**
 * 会话列表（底部第 2 个 tab）
 *
 * 数据源：腾讯云 IM 会话列表（登录后云端自动同步，换设备不丢）。
 * 官方账号会话永远置顶：本地排序 + 云端 pinConversation 双保险。
 *
 * 加载策略（避免冷启动空屏）：
 * 1. 进页立刻渲染 [ConversationCache] 里的上一轮快照 —— 无网络/SDK 未就绪也秒出；
 * 2. IM 已登录则拉 SDK 会话列表，回来后覆盖并回写缓存；
 * 3. 未登录（冷启动同步中）显示"正在同步会话…"，登录成功事件到达后自动重拉。
 */
class ConversationFragment : TopPmFragment<MainVM>() {

    companion object {
        private const val TAG = "ConversationFragment"
        //缓存是否已在本次进程内渲染过（进程内第二次切 tab 不必再读盘）
        private var cacheRendered = false
    }

    override fun layoutId() = R.layout.fragment_conversation

    override fun initVM() = MainVM()

    private val binding get() = mBinding as FragmentConversationBinding

    private lateinit var adapter: ConversationAdapter

    override fun initShiTu() {
        adapter = ConversationAdapter(requireContext()).apply {
            emptyText = getString(R.string.im_conversation_empty)
            onItemClick = { item ->
                val name = displayName(item).takeIf { it.isNotBlank() }
                    ?: getString(R.string.im_unknown_user)
                openPage(ChatFragment.newInstance(item.peerId, name, displayAvatar(item)))
            }
        }
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter
        binding.rvList.itemAnimator = null
    }

    override fun getClickViews(): List<View> = emptyList()

    override fun onSingleClick(v: View?) {}

    override fun onVisible() {
        super.onVisible()
        showCachedIfNeeded()
        loadConversations()
        ImUtil.pinOfficialConversation()
    }

    /** 先拿本地快照上屏：SDK 没就绪也不空屏 */
    private fun showCachedIfNeeded() {
        if (cacheRendered) return
        val cached = ConversationCache.read(requireContext())
        if (cached.isNotEmpty()) {
            adapter.showEmptyTip = true
            binding.tvEmpty.visibility = View.GONE
            adapter.initData(cached)
            cacheRendered = true
        }
    }

    private fun loadConversations() {
        if (!ImManager.loggedIn) {
            //SDK 还没登录完成（冷启动同步中）：有缓存就静默等登录事件，没缓存提示同步中
            if (adapter.itemCount == 0) {
                binding.tvEmpty.text = getString(
                    if (GlobalConfig.session.isBlank()) R.string.im_conversation_need_login
                    else R.string.im_conversation_syncing
                )
                binding.tvEmpty.visibility = View.VISIBLE
                adapter.showEmptyTip = false
                adapter.initData(emptyList())
            }
            return
        }
        adapter.showEmptyTip = true
        binding.tvEmpty.visibility = View.GONE
        try {
            V2TIMManager.getConversationManager()
                .getConversationList(0, 100, object : V2TIMValueCallback<V2TIMConversationResult> {
                    override fun onSuccess(t: V2TIMConversationResult?) {
                        if (!isAdded) return
                        val items = ImUtil.toItems(t?.conversationList ?: emptyList())
                        adapter.initData(items)
                        ConversationCache.save(requireContext(), items)
                        cacheRendered = true
                    }

                    override fun onError(code: Int, desc: String?) {
                        log(TAG, "会话列表拉取失败 code=$code desc=$desc")
                    }
                })
        } catch (e: Exception) {
            log(TAG, "会话列表异常 ${e.message}")
        }
    }

    /** 新消息到达 / 未读变化 / IM 登录完成：重拉会话列表 */
    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        if (!isAdded) return
        when (event.what) {
            EventConstant.EVENT_IM_NEW_MESSAGE,
            EventConstant.EVENT_IM_LOGIN_SUCCESS -> loadConversations()
        }
    }
}

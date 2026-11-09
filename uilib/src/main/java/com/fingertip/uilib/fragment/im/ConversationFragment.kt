package com.fingertip.uilib.fragment.im

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.ConversationAdapter
import com.fingertip.uilib.databinding.FragmentConversationBinding
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
 * 新消息/会话变化走 EventBus 增量重拉，不加 listener 生命周期管理负担。
 */
class ConversationFragment : TopPmFragment<MainVM>() {

    companion object {
        private const val TAG = "ConversationFragment"
    }

    override fun layoutId() = R.layout.fragment_conversation

    override fun initVM() = MainVM()

    private val binding get() = mBinding as FragmentConversationBinding

    private lateinit var adapter: ConversationAdapter

    override fun initShiTu() {
        adapter = ConversationAdapter(requireContext()).apply {
            emptyText = getString(R.string.im_conversation_empty)
            onItemClick = { conv ->
                val peerId = ImUtil.userIdOfConversation(conv)
                //用解析后的昵称/头像（IM 资料缺失时适配器已向后端补拉）
                val name = adapter.displayName(conv)
                    .takeIf { it.isNotBlank() } ?: getString(R.string.im_unknown_user)
                val avatar = adapter.displayAvatar(conv)
                openPage(
                    ChatFragment.newInstance(peerId, name, avatar)
                )
            }
        }
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter
        binding.rvList.itemAnimator = null
    }

    override fun getClickViews(): List<View> = emptyList()

    override fun onSingleClick(v: View?) {}

    override fun initObserver() {
        super.initObserver()
    }

    override fun onVisible() {
        super.onVisible()
        loadConversations()
        ImUtil.pinOfficialConversation()
    }

    private fun loadConversations() {
        if (!ImManager.loggedIn) {
            //未登录/未连上：显示引导文案，登录后会因新消息事件自动重拉
            binding.tvEmpty.text = getString(
                if (GlobalConfig.session.isBlank()) R.string.im_conversation_need_login
                else R.string.im_conversation_empty
            )
            binding.tvEmpty.visibility = View.VISIBLE
            adapter.showEmptyTip = false
            adapter.initData(emptyList())
            return
        }
        adapter.showEmptyTip = true
        binding.tvEmpty.visibility = View.GONE
        try {
            V2TIMManager.getConversationManager()
                .getConversationList(0, 100, object : V2TIMValueCallback<V2TIMConversationResult> {
                    override fun onSuccess(t: V2TIMConversationResult?) {
                        val list = t?.conversationList ?: emptyList()
                        adapter.initData(ImUtil.sortConversations(list))
                    }

                    override fun onError(code: Int, desc: String?) {
                        log(TAG, "会话列表拉取失败 code=$code desc=$desc")
                    }
                })
        } catch (e: Exception) {
            log(TAG, "会话列表异常 ${e.message}")
        }
    }

    /** 新消息到达 / 未读变化：重拉会话列表（官方置顶在 sortConversations 里保证） */
    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        if (!isAdded) return
        when (event.what) {
            EventConstant.EVENT_IM_NEW_MESSAGE -> loadConversations()
        }
    }
}

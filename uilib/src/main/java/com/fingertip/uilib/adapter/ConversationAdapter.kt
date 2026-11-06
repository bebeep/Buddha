package com.fingertip.uilib.adapter

import android.view.View
import android.view.ViewOutlineProvider
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.top.TopRcAdapter.TopRcViewHolder
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemConversationBinding
import com.fingertip.uilib.im.ImConfig
import com.fingertip.uilib.im.ImMsgUtil
import com.fingertip.uilib.im.ImUtil
import com.fingertip.baselib.R as BaseR
import com.tencent.imsdk.v2.V2TIMConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 会话列表。官方账号排最前由 [ImUtil.sortConversations] 保证
 */
class ConversationAdapter(context: android.content.Context) :
    TopRcAdapter<V2TIMConversation, ConversationAdapter.Holder>(context) {

    var onItemClick: ((V2TIMConversation) -> Unit)? = null

    override fun initLayoutId(viewType: Int) = R.layout.item_conversation

    class Holder(view: View, binding: com.fingertip.uilib.databinding.ItemConversationBinding) :
        TopRcViewHolder(view) {
        val b: ItemConversationBinding = binding
    }

    /** 后端资料兜底：IM 侧没设昵称/头像时用它补（userId -> 资料） */
    private data class Profile(val name: String, val avatar: String)
    private val profiles = HashMap<String, Profile>()
    private val fetching = HashSet<String>()

    /** 展示名：IM showName 优先；为空或就是原始 userId 时用后端昵称兜底 */
    fun displayName(conv: V2TIMConversation): String {
        val userId = ImUtil.userIdOfConversation(conv)
        conv.showName?.takeIf { it.isNotBlank() && it != userId }?.let { return it }
        profiles[userId]?.name?.takeIf { it.isNotBlank() }?.let { return it }
        return ""
    }

    /** 展示头像：IM faceUrl 优先，为空时用后端头像兜底 */
    fun displayAvatar(conv: V2TIMConversation): String {
        conv.faceUrl?.takeIf { it.isNotBlank() }?.let { return it }
        return profiles[ImUtil.userIdOfConversation(conv)]?.avatar ?: ""
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder {
        val v = android.view.LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false)
        val b = ItemConversationBinding.bind(v)
        //头像强制圆形裁切：占位图/兜底图不走 Glide transform，直接在 View 层裁
        b.ivAvatar.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
        b.ivAvatar.clipToOutline = true
        return Holder(v, b)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val conv = get(position) ?: return
        val b = holder.b
        val userId = ImUtil.userIdOfConversation(conv)

        val official = ImMsgUtil.isOfficialConv(userId)
        b.tvOfficialBadge.visibility = if (official) View.VISIBLE else View.GONE

        //昵称：IM 资料缺失时异步向后端拉，回来后刷新（profiles 缓存，只拉一次）
        val name = displayName(conv)
        if (name.isBlank()) ensureProfile(userId)
        b.tvName.text = name.ifBlank { context.getString(R.string.im_unknown_user) }

        val avatarUrl = displayAvatar(conv)
        b.ivAvatar.loadImg(
            avatarUrl.ifBlank { null },
            holder = if (official) R.drawable.icon_im_official_avatar else BaseR.mipmap.icon_default_head,
            transform = CircleCrop()
        )
        b.tvSummary.text = conv.lastMessage?.let { ImMsgUtil.summaryOf(it) } ?: ""
        b.tvTime.text = ImMsgUtil.conversationTime(conv.lastMessage?.timestamp ?: 0)
        val unread = conv.unreadCount
        if (unread > 0) {
            b.tvUnread.visibility = View.VISIBLE
            b.tvUnread.text = if (unread > 99) "99+" else unread.toString()
        } else {
            b.tvUnread.visibility = View.GONE
        }
        b.root.setOnClickListener { onItemClick?.invoke(conv) }
    }

    /** 会话名/头像缺失时向后端补拉资料，成功后缓存并刷新列表 */
    private fun ensureProfile(userId: String) {
        if (profiles.containsKey(userId)) return
        if (!fetching.add(userId)) return
        val accountId = ImConfig.accountIdOf(userId)
        if (accountId <= 0 || GlobalConfig.session.isBlank()) {
            fetching.remove(userId)
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val rsp = NetManager.getApi().getUserInfo(accountId = accountId)
                if (rsp.code == 0) {
                    val p = rsp.data
                    profiles[userId] = Profile(
                        p?.userName?.takeIf { it.isNotBlank() } ?: "",
                        p?.avatar ?: ""
                    )
                    notifyDataSetChanged()
                }
            } catch (_: Exception) {
            } finally {
                fetching.remove(userId)
            }
        }
    }
}

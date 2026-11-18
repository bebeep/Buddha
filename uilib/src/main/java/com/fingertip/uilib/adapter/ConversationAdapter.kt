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
import com.fingertip.uilib.im.ConversationItem
import com.fingertip.uilib.im.ImConfig
import com.fingertip.baselib.R as BaseR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 会话列表。数据是 [ConversationItem]（SDK 会话已转成可缓存的展示模型，见 ImUtil.toItems），
 * 官方账号排最前由上游排序保证。
 */
class ConversationAdapter(context: android.content.Context) :
    TopRcAdapter<ConversationItem, ConversationAdapter.Holder>(context) {

    var onItemClick: ((ConversationItem) -> Unit)? = null

    override fun initLayoutId(viewType: Int) = R.layout.item_conversation

    class Holder(view: View, binding: com.fingertip.uilib.databinding.ItemConversationBinding) :
        TopRcViewHolder(view) {
        val b: ItemConversationBinding = binding
    }

    /** 后端资料兜底：IM 侧没设昵称/头像时用它补（peerId -> 资料） */
    private data class Profile(val name: String, val avatar: String)
    private val profiles = HashMap<String, Profile>()
    private val fetching = HashSet<String>()

    /** 展示名：IM showName 优先；为空时用后端昵称兜底 */
    fun displayName(item: ConversationItem): String {
        item.name.takeIf { it.isNotBlank() }?.let { return it }
        profiles[item.peerId]?.name?.takeIf { it.isNotBlank() }?.let { return it }
        return ""
    }

    /** 展示头像：IM faceUrl 优先，为空时用后端头像兜底 */
    fun displayAvatar(item: ConversationItem): String {
        item.avatar.takeIf { it.isNotBlank() }?.let { return it }
        return profiles[item.peerId]?.avatar ?: ""
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
        val item = get(position) ?: return
        val b = holder.b

        b.tvOfficialBadge.visibility = if (item.isOfficial) View.VISIBLE else View.GONE

        //昵称：IM 资料缺失时异步向后端拉，回来后刷新（profiles 缓存，只拉一次）
        val name = displayName(item)
        if (name.isBlank()) ensureProfile(item.peerId)
        b.tvName.text = name.ifBlank { context.getString(R.string.im_unknown_user) }

        val avatarUrl = displayAvatar(item)
        b.ivAvatar.loadImg(
            avatarUrl.ifBlank { null },
            holder = if (item.isOfficial) R.drawable.icon_im_official_avatar else BaseR.mipmap.icon_default_head,
            transform = CircleCrop()
        )
        b.tvSummary.text = item.summary
        b.tvTime.text = item.timeText
        if (item.unread > 0) {
            b.tvUnread.visibility = View.VISIBLE
            b.tvUnread.text = if (item.unread > 99) "99+" else item.unread.toString()
        } else {
            b.tvUnread.visibility = View.GONE
        }
        b.root.setOnClickListener { onItemClick?.invoke(item) }
    }

    /** 会话名/头像缺失时向后端补拉资料，成功后缓存并刷新列表 */
    private fun ensureProfile(peerId: String) {
        if (profiles.containsKey(peerId)) return
        if (!fetching.add(peerId)) return
        val accountId = ImConfig.accountIdOf(peerId)
        if (accountId <= 0 || GlobalConfig.session.isBlank()) {
            fetching.remove(peerId)
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val rsp = NetManager.getApi().getUserInfo(accountId = accountId)
                if (rsp.code == 0) {
                    val p = rsp.data
                    profiles[peerId] = Profile(
                        p?.userName?.takeIf { it.isNotBlank() } ?: "",
                        p?.avatar ?: ""
                    )
                    notifyDataSetChanged()
                }
            } catch (_: Exception) {
            } finally {
                fetching.remove(peerId)
            }
        }
    }
}

package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.UserBlockData
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemBlockUserBinding

/**
 * 已拉黑用户列表
 *
 * 昵称头像由服务端 join account_info 带出；真拿到空值时降级成「用户 #id」，
 * 至少让用户知道拉黑过几个、能解除，而不是显示一片空白。
 */
class BlockUserAdapter(
    context: Context,
    private val onUnblock: (UserBlockData) -> Unit
) : TopRcAdapter<UserBlockData, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_block_user

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemBlockUserBinding>()
        val item = get(position) ?: return

        binding.ivAvatar.loadHead(item.targetAvatar)
        binding.tvName.text = item.targetUserName?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.block_list_name_unknown, item.targetAccountId)

        binding.tvDate.text = item.createDate?.takeIf { it.length >= 10 }
            ?.let { context.getString(R.string.block_list_date, it.substring(0, 10)) }
            ?: ""

        binding.tvUnblock.setOnClickListener { onUnblock.invoke(item) }
    }
}

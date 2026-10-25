package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.PrayingBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemPrayingMineBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 我的祈福列表 Adapter。显示内容、祝福者昵称、时间，支持删除
 */
class MyPrayingAdapter(
    context: Context,
    private val onDelete: (PrayingBean) -> Unit
) : TopRcAdapter<PrayingBean, TopRcAdapter.TopRcViewHolder>(context) {

    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    override fun initLayoutId(viewType: Int): Int = R.layout.item_praying_mine

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemPrayingMineBinding>()
        val item = get(position) ?: return

        binding.tvContent.text = item.content
        val names = item.blessUserNames.orEmpty()
        binding.tvBlessers.text = if (names.isEmpty()) "暂无人祝福"
        else names.joinToString("、").let { if (it.length > 24) it.take(24) + "…" else it }
        binding.tvTime.text = friendlyTime(item.createDate)
        binding.tvBlessCount.text = "${item.blessCount}人已祝福"
        binding.tvDelete.visibility =
            if (item.accountId == com.fingertip.baselib.constant.GlobalConfig.userData?.accountId) View.VISIBLE else View.GONE
        binding.tvDelete.setOnClickListener { onDelete(item) }
    }

    private fun friendlyTime(createDate: String?): String {
        if (createDate.isNullOrBlank()) return ""
        return try {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).parse(createDate)?.time ?: return ""
            val diff = System.currentTimeMillis() - time
            val hour = diff / 3600000
            when {
                diff < 60000 -> "刚刚"
                diff < 3600000 -> "${diff / 60000}分钟前"
                hour < 24 -> "${hour}小时前"
                hour < 24 * 30 -> "${hour / 24}天前"
                else -> format.format(Date(time))
            }
        } catch (e: Exception) {
            ""
        }
    }
}

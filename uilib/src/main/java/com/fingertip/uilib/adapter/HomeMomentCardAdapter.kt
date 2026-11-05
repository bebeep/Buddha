package com.fingertip.uilib.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemHomeMomentCardBinding
import com.fingertip.uilib.utils.DailyQuotes
import java.util.Locale
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R as BaseR

/**
 * 首页「佛友圈精选」横滑卡。
 *
 * 三类内容共用一张卡：官方开示、社区热帖、禅语日签（兜底）。
 * 用标签而不是不同版式区分，一屏里三种内容看起来才是一个整体。
 */
class HomeMomentCardAdapter : RecyclerView.Adapter<HomeMomentCardAdapter.Holder>() {

    /**
     * 一张卡的展示数据。entity 非空表示是真实动态，点了进动态详情；
     * quote 非空表示是禅语兜底卡，点了弹日签详情（本地内容，没有动态 id）。
     */
    class Card(
        val text: String,
        val author: String,
        val likeCount: Int,
        val label: String,
        val official: Boolean,
        val entity: MomentEntity? = null,
        val quote: DailyQuotes.Quote? = null
    )

    private var cards: List<Card> = emptyList()

    var onItemClick: ((Card) -> Unit)? = null

    fun submit(list: List<Card>) {
        cards = list
        notifyDataSetChanged()
    }

    /**
     * 把服务端动态 + 禅语兜底拼成 3 张卡。
     *
     * 服务端给了几条就显示几条，不够的用当天禅语补——首页永不出现空态。
     */
    fun build(moments: List<MomentEntity>?): List<Card> {
        val list = ArrayList<Card>()
        moments.orEmpty().take(CARD_COUNT).forEach { entity ->
            val raw = entity.textContent.orEmpty().trim()
            // 官方帖正文是「句子 + 空行 + 出处」，卡片只留句子，出处交给标签行
            val brief = raw.split("\n\n——").firstOrNull()?.trim().orEmpty()
                .ifEmpty { raw }
            val official = entity.roleType == 2
            list.add(
                Card(
                    text = brief,
                    author = entity.postUserName.orEmpty().ifBlank { "佛友" },
                    likeCount = entity.likeCount,
                    label = if (official) "官方开示" else "社区热帖",
                    official = official,
                    entity = entity
                )
            )
        }
        if (list.size < CARD_COUNT) {
            DailyQuotes.wrap(CARD_COUNT - list.size).forEach { quote ->
                list.add(
                    Card(
                        text = quote.text,
                        author = quote.source,
                        likeCount = 0,
                        label = "禅语日签",
                        official = false,
                        quote = quote
                    )
                )
            }
        }
        return list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemHomeMomentCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun getItemCount(): Int = cards.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val card = cards[position]
        val binding = holder.binding
        binding.tvSource.text = card.label
        binding.tvContent.text = card.text
        binding.tvAuthor.text = card.author
        binding.vSourceDot.setBackgroundResource(
            if (card.official) R.drawable.bg_moment_dot_gold else R.drawable.bg_moment_dot_muted
        )
        binding.tvSource.setTextColor(if (card.official) TopApplication.instance.getColor(BaseR.color.c_gold) else TopApplication.instance.getColor(BaseR.color.c_text_second))
        // 禅语卡没有点赞数，显示出处已占用右侧位置，这里直接隐藏
        binding.tvLike.visibility = if (card.likeCount > 0) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvLike.text = formatCount(card.likeCount)
        binding.root.setOnClickListener { onItemClick?.invoke(card) }
    }

    private fun formatCount(count: Int): String = when {
        count >= 10_000 -> String.format(Locale.getDefault(), "%.1fw", count / 10_000f)
        count >= 1_000 -> String.format(Locale.getDefault(), "%.1fk", count / 1_000f)
        else -> count.toString()
    }

    class Holder(val binding: ItemHomeMomentCardBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        const val CARD_COUNT = 3
    }
}

package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.UserLevelConfig
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.LevelUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemLevelRowBinding
import com.fingertip.uilib.utils.LevelUi
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R as BaseR

/**
 * 全部等级表的一行
 * @param config 该级配置（level/levelTag/exp）
 * @param prevExp 上一级的累计门槛，用来算「本级 +N」
 * @param isCurrent 是否当前所在等级（左侧金色竖条 + 金色文字）
 */
data class LevelRow(
    val config: UserLevelConfig,
    val prevExp: Int,
    val isCurrent: Boolean
)

/**
 * 等级表列表
 *
 * 纯展示，数据在 UserLevelListFragment 里按 LevelUtil.configs 组装好再灌进来。
 */
class LevelRowAdapter(context: Context) :
    TopRcAdapter<LevelRow, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_level_row

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemLevelRowBinding>()
        val item = get(position) ?: return
        val level = item.config.level

        //等级徽章：服务端按级下发的药丸图（与全 App 各处同一套图）；
        //配置缺失时回退到 tv_badge 段色文字牌
        if (LevelUtil.tagUrlOf(level).isNotBlank()) {
            binding.ivBadge.visibility = View.VISIBLE
            binding.tvBadge.visibility = View.GONE
            LevelUi.bindTag(binding.ivBadge, level)
        } else {
            binding.ivBadge.visibility = View.GONE
            binding.tvBadge.visibility = View.VISIBLE
            LevelUi.applyTextBadge(binding.tvBadge, level, radiusDp = 5f, horizontalPadDp = 8)
        }

        binding.tvTag.text = item.config.levelTag?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.level_row_level_only, level)
        //当前等级用金色，其余用主文字色（baseLib 的 c_* 颜色 uilib 的 R 取不到，这里写字面值）
        binding.tvTag.setTextColor(if (item.isCurrent) TopApplication.instance.getColor(BaseR.color.c_gold) else TopApplication.instance.getColor(BaseR.color.c_text_main))

        //lv0 是起点（0 经验），「本级 +0」没意义，直接不显示
        if (level <= 0) {
            binding.tvExp.text = context.getString(R.string.level_row_start)
            binding.tvDelta.visibility = View.GONE
        } else {
            binding.tvExp.text = context.getString(R.string.level_row_exp, item.config.exp)
            val delta = item.config.exp - item.prevExp
            binding.tvDelta.text = context.getString(R.string.level_row_delta, delta)
            binding.tvDelta.visibility = View.VISIBLE
        }

        binding.vCurrent.visibility = if (item.isCurrent) View.VISIBLE else View.INVISIBLE
    }
}

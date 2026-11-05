package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.LampRecordData
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemLampRecordBinding
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.R as BaseR

/**
 * 供灯记录列表
 *
 * 一条记录 = 一次点燃或一次续燃（服务端续燃是累加到原灯上，但订单号是本条的，所以仍可追溯）。
 * 灯具/灯油名称由服务端 join 好下发，客户端不再按 id 反查——少一次请求，也不会出现"未知装扮"。
 */
class LampHistoryAdapter(context: Context) :
    TopRcAdapter<LampRecordData, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_lamp_record

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemLampRecordBinding>()
        val item = get(position) ?: return

        val lampName = item.lampName?.takeIf { it.isNotBlank() } ?: "默认灯"
        val oilName = item.oilName?.takeIf { it.isNotBlank() } ?: "${item.oilDays} 天灯油"
        //多佛像后灯落在具体佛像前；buddhaId=0 的历史数据没有佛像名，不硬凑
        val buddhaName = item.buddhaName?.takeIf { it.isNotBlank() }
        binding.tvNo.text = if (buddhaName != null) "$oilName · $lampName · $buddhaName"
                            else "$oilName · $lampName"

        binding.tvDate.text = "${shortTime(item.lightDate)} 点燃 · ${shortTime(item.expireDate)} 燃尽"

        //订单号是恢复购买与客诉追溯的唯一凭证，空值说明是后台补发的记录
        binding.tvOrder.text = item.iapOrderNo?.takeIf { it.isNotBlank() }
            ?.let { "订单 $it" } ?: "后台发放"

        val burning = item.lampStatus == 1
        binding.tvState.text = if (burning) "燃烧中" else "已燃尽"
        binding.tvState.setTextColor(if (burning) TopApplication.instance.getColor(BaseR.color.c_gold) else TopApplication.instance.getColor(BaseR.color.c_text_second))
        //燃尽的记录整行压暗，一眼能看出哪些已经结束
        binding.vDot.alpha = if (burning) 1f else 0.3f
        binding.tvNo.alpha = if (burning) 1f else 0.6f
    }

    /**
     * 服务端下发 "yyyy-MM-dd HH:mm:ss"，列表只要到分钟
     */
    private fun shortTime(time: String?): String =
        time?.takeIf { it.length >= 16 }?.substring(0, 16) ?: "-"
}

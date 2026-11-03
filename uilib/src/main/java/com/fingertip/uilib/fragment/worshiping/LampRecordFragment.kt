package com.fingertip.uilib.fragment.worshiping

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.LampDetailData
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.LampHistoryAdapter
import com.fingertip.uilib.databinding.FragmentLampRecordBinding
import com.fingertip.uilib.fragment.MainFragment
import com.fingertip.uilib.fragment.shop.ShopFragment
import com.fingertip.uilib.viewmodel.LampVM

/**
 * 供灯记录
 *
 * 上半：当前灯状态 + 一键去续燃（灯灭前的那一下转化，就靠这个入口）
 * 下半：历史记录，每条绑 IAP 订单号
 *
 * 为什么订单号要露出来：用户换手机后"恢复购买"要靠它、客服排查也要靠它。
 * 藏在后台不显示的结果就是每次客诉都要人工查库。
 */
class LampRecordFragment : TopPmFragment<LampVM>() {

    override fun layoutId() = R.layout.fragment_lamp_record

    override fun initVM(): LampVM = provideVM()

    private val binding get() = mBinding as FragmentLampRecordBinding

    private lateinit var adapter: LampHistoryAdapter

    override fun initShiTu() {
        adapter = LampHistoryAdapter(requireContext())
        binding.rcList.layoutManager = LinearLayoutManager(requireContext())
        binding.rcList.adapter = adapter
        binding.rcList.itemAnimator = null

        mViewModel.loadStatus()
        mViewModel.loadHistory(0)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.statusResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            renderCurrent(if (result.success) result.data else null)
        }

        mViewModel.historyResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val list = result.data.orEmpty()
            adapter.initData(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun renderCurrent(lamp: LampDetailData?) {
        val data = lamp ?: LampDetailData()
        when {
            data.isNone() -> {
                binding.tvCurrentName.setText(R.string.lamp_record_none)
                binding.tvCurrentDesc.setText(R.string.lamp_record_none_desc)
                binding.tvCurrentAction.setText(R.string.worship_light_lamp)
            }
            data.isDying() -> {
                binding.tvCurrentName.text = data.displayName()
                binding.tvCurrentDesc.setText(R.string.lamp_record_relight_desc)
                binding.tvCurrentAction.setText(R.string.worship_relight)
            }
            else -> {
                binding.tvCurrentName.text = data.displayName()
                binding.tvCurrentDesc.text = getString(R.string.lamp_record_remain, data.remainHours)
                binding.tvCurrentAction.setText(R.string.worship_relight)
            }
        }
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack, binding.tvCurrentAction)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            //续燃只有一条路径：去商城买灯油，服务端发货时给现有灯顺延
            binding.tvCurrentAction ->
                (parentFragment as? MainFragment)?.start(ShopFragment.newInstance(ShopDressConfig.TYPE_OIL))
        }
    }
}

package com.fingertip.uilib.fragment.blessing

import android.view.View
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.fingertip.baselib.bean.PRAYING_DAILY_BLESS_LIMIT
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.PrayingCardAdapter
import com.fingertip.uilib.databinding.FragmentBlessingBinding
import com.fingertip.uilib.dialog.PrayingCardDialog
import com.fingertip.uilib.dialog.PrayingEditDialog
import com.fingertip.uilib.dialog.PrayingRuleDialog
import com.fingertip.uilib.viewmodel.BlessingVM

/**
 * 祈福主页，对应原型「祈福-主页」与 blessing 一组接口
 *
 * - 悬浮福卡墙（两列瀑布+随机倾斜），点卡片弹「拆福卡」送祝福
 * - 右下角「祈」发布福卡；每日祝福5次，服务端限制
 */
class BlessingFragment : TopPmFragment<BlessingVM>() {

    companion object {
        private const val TAG = "BlessingFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_blessing

    override fun initVM(): BlessingVM = provideVM()

    private val binding get() = mBinding as FragmentBlessingBinding

    private lateinit var adapter: PrayingCardAdapter

    private var cardDialog: PrayingCardDialog? = null
    private var editDialog: PrayingEditDialog? = null
    private var ruleDialog: PrayingRuleDialog? = null

    override fun initShiTu() {
        adapter = PrayingCardAdapter(requireContext()) { praying ->
            showCardDialog(praying)
        }
        binding.rcWall.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.rcWall.adapter = adapter

        mViewModel.loadWall()
        mViewModel.loadStatus()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.wallResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                val list = result.data.orEmpty()
                adapter.initData(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            } else {
                log(TAG, "福卡墙获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "福卡加载失败，请稍后再试")
            }
        }

        mViewModel.statusResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                binding.tvTodayStatus.text =
                    "今日已祝福 ${result.data ?: 0}/$PRAYING_DAILY_BLESS_LIMIT 次"
            }
        }

        mViewModel.postResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                ToastUtil.showMessage("祈福成功")
                editDialog?.onPostSuccess()
                mViewModel.loadWall()
            } else {
                editDialog?.onPostFailed()
                ToastUtil.showMessage(result.errorMsg ?: "祈福失败，请稍后再试")
            }
        }

        mViewModel.blessResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                cardDialog?.onBlessSuccess()
                mViewModel.loadStatus()
            } else {
                cardDialog?.onBlessFailed(result.errorMsg)
                ToastUtil.showMessage(result.errorMsg ?: "祝福失败，请稍后再试")
            }
        }
    }

    private fun showCardDialog(praying: com.fingertip.baselib.bean.PrayingBean) {
        val dialog = cardDialog ?: PrayingCardDialog(requireContext()).also {
            it.onBless = { praying -> mViewModel.blessPraying(praying.id) }
            cardDialog = it
        }
        dialog.render(praying)
        dialog.show()
    }

    private fun showEditDialog() {
        val dialog = editDialog ?: PrayingEditDialog(requireContext()).also {
            it.onSubmit = { content, coverType -> mViewModel.postPraying(content, coverType) }
            editDialog = it
        }
        dialog.show()
    }

    private fun showRuleDialog() {
        val dialog = ruleDialog ?: PrayingRuleDialog(requireContext()).also { ruleDialog = it }
        dialog.show()
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack,
        binding.tvRule,
        binding.tvAllPraying,
        binding.tvPostPraying
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvRule -> showRuleDialog()
            binding.tvAllPraying ->
                (parentFragment as? com.fingertip.uilib.fragment.MainFragment)?.start(
                    PrayingMineFragment.newInstance(isAll = true)
                )
            binding.tvPostPraying -> showEditDialog()
        }
    }
}

package com.fingertip.uilib.fragment.deed

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.DeedBean
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.DeedDonateAdapter
import com.fingertip.uilib.databinding.FragmentDeedDetailBinding
import com.fingertip.uilib.dialog.DeedDonateDialog
import com.fingertip.uilib.viewmodel.GoodDeedVM

/**
 * 行善详情，对应原型「行善-详情页」：
 * 标题/内容/进度/善款说明/捐赠列表（分页），底部捐功德币与分享助力
 */
class GoodDeedDetailFragment : TopPmFragment<GoodDeedVM>() {

    companion object {
        private const val ARG_DEED_ID = "arg_deed_id"
        private const val TAG = "GoodDeedDetailFragment"

        fun newInstance(deedId: Int): GoodDeedDetailFragment {
            return GoodDeedDetailFragment().apply {
                arguments = Bundle().apply { putInt(ARG_DEED_ID, deedId) }
            }
        }
    }

    override fun layoutId(): Int = R.layout.fragment_deed_detail

    override fun initVM(): GoodDeedVM = provideVM()

    private val binding get() = mBinding as FragmentDeedDetailBinding

    private lateinit var donateAdapter: DeedDonateAdapter
    private var deedId = 0
    private var deed: DeedBean? = null
    private var donateDialog: DeedDonateDialog? = null

    override fun initShiTu() {
        deedId = arguments?.getInt(ARG_DEED_ID, 0) ?: 0
        donateAdapter = DeedDonateAdapter(requireContext())
        binding.rcDonate.layoutManager = LinearLayoutManager(requireContext())
        binding.rcDonate.adapter = donateAdapter
        mViewModel.loadDetail(deedId)
        mViewModel.loadDonateList(deedId, 0)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.detailResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                render(result.data ?: return@observe)
            } else {
                log(TAG, "善行详情获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败")
            }
        }

        mViewModel.donateListResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                donateAdapter.initData(result.data.orEmpty())
            }
        }

        mViewModel.donateResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                ToastUtil.showMessage("捐赠成功，功德无量")
                donateDialog?.onDonateSuccess()
                mViewModel.loadDetail(deedId)
                mViewModel.loadDonateList(deedId, 0)
            } else {
                donateDialog?.onDonateFailed()
                ToastUtil.showMessage(result.errorMsg ?: "捐赠失败，请稍后再试")
            }
        }

        mViewModel.shareResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                ToastUtil.showMessage("感谢助力，把善行分享给更多人吧")
            } else {
                ToastUtil.showMessage(result.errorMsg ?: "分享失败")
            }
        }
    }

    private fun render(item: DeedBean) {
        deed = item
        binding.tvDeedTitle.text = item.title
        val officialTag = if (item.isOfficial == 1) "[官方]" else ""
        val urgentTag = if (item.isUrgent == 1) "[急]" else ""
        binding.tvDeedTitle.text = "$urgentTag$officialTag${item.title}"
        binding.tvAuthor.text = if (item.isOfficial == 1) "指尖佛官方" else (item.accountInfo?.userName ?: "佛友")
        binding.tvDate.text = item.createDate?.take(10).orEmpty()
        binding.tvContent.text = item.content

        binding.progress.max = item.targetCoins.coerceAtLeast(1)
        binding.progress.progress = item.donatedCoins.coerceAtMost(item.targetCoins)
        binding.tvProgress.text =
            "已捐赠 ${item.donatedCoins}/${item.targetCoins} 功德币 · ${item.donorCount}人参与 · 分享${item.shareCount}次"

        val statusText = when (item.status) {
            0 -> "进行中：达到求助目标后，平台会通过官方渠道转赠善款，全程接受佛友监督并实时公布。"
            1 -> "善款已筹集完毕，等待平台打款。"
            2 -> "平台正在打款，请留意后续进展公示。"
            else -> "善款已完成捐赠，功德无量。"
        }
        binding.tvStatusTip.text = statusText

        //已结束的善行禁用捐赠
        binding.tvDonate.isEnabled = item.status == 0
        binding.tvDonate.alpha = if (item.status == 0) 1f else 0.5f
        if (item.myDonateCount > 0) {
            binding.tvDonate.text = "捐功德币(已捐${item.myDonateCoins})"
        }
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack, binding.tvDonate, binding.tvShare
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvDonate -> showDonateDialog()
            binding.tvShare -> {
                deed?.let { mViewModel.share(it.id) }
            }
        }
    }

    private fun showDonateDialog() {
        val dialog = donateDialog ?: DeedDonateDialog(requireContext()).also {
            it.onDonate = { coins, message -> mViewModel.donate(deedId, coins, message) }
            donateDialog = it
        }
        dialog.render(deed?.title)
        dialog.show()
    }
}

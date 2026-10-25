package com.fingertip.uilib.fragment.worshiping

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fingertip.baselib.bean.DonateBuddhaBean
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.DonateBuddhaAdapter
import com.fingertip.uilib.databinding.FragmentDonateBinding
import com.fingertip.uilib.dialog.DonateCoinDialog
import com.fingertip.uilib.dialog.DonateRecordDialog
import com.fingertip.uilib.viewmodel.DonateVM

/**
 * 功德捐赠页，对应原型「功德/功德详情/功德捐赠/抢福袋」与 donate 一组接口
 *
 * - 佛像横滑单选，下方功德数据卡（在供/累计/我的贡献/排名/贡献榜前3）
 * - 捐功德币（服务端返回最新账户余额并同步 GlobalConfig）、抢福袋、捐赠记录
 */
class DonateFragment : TopPmFragment<DonateVM>() {

    companion object {
        private const val TAG = "DonateFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_donate

    override fun initVM(): DonateVM = provideVM()

    private val binding get() = mBinding as FragmentDonateBinding

    private lateinit var buddhaAdapter: DonateBuddhaAdapter
    private var current: DonateBuddhaBean? = null

    private var coinDialog: DonateCoinDialog? = null
    private var recordDialog: DonateRecordDialog? = null

    override fun initShiTu() {
        buddhaAdapter = DonateBuddhaAdapter(requireContext()) { buddha ->
            current = buddha
            renderBuddha(buddha)
        }
        binding.rcBuddha.adapter = buddhaAdapter
        mViewModel.loadBuddhaList()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.buddhaListResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                val list = result.data.orEmpty()
                buddhaAdapter.initData(list)
                buddhaAdapter.selectFirst()
            } else {
                log(TAG, "功德佛像获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败，请稍后再试")
            }
        }

        mViewModel.donateResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                ToastUtil.showMessage("捐赠成功，功德无量")
                coinDialog?.onDonateSuccess()
                //服务端返回最新账户信息，同步余额
                result.data?.let { com.fingertip.baselib.constant.GlobalConfig.userData = it }
                mViewModel.loadBuddhaList()
            } else {
                coinDialog?.onDonateFailed()
                ToastUtil.showMessage(result.errorMsg ?: "捐赠失败，请稍后再试")
            }
        }

        mViewModel.luckyBagResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                val bag = result.data
                val text = if (bag != null && bag.hitRewardCoin > 0)
                    "福袋开出 ${bag.rewardCoin} 功德币，命中奖池额外 +${bag.hitRewardCoin}！"
                else "福袋开出 ${bag?.rewardCoin ?: 0} 功德币"
                ToastUtil.showMessage(text)
                mViewModel.loadBuddhaList()
            } else {
                ToastUtil.showMessage(result.errorMsg ?: "福袋已抢完，明天再来")
            }
        }
    }

    private fun renderBuddha(item: DonateBuddhaBean) {
        binding.tvBuddhaName.text = item.buddhaName
        binding.tvBuddhaIntro.text = item.buddhaIntro
        binding.tvOwnCount.text = "${item.ownCount}"
        binding.tvTotalIncome.text = "${item.totalIncome}"
        binding.tvTodayReward.text = "${item.todayRewardCount}"
        binding.tvMyContribution.text = buildString {
            append("我的累计功德 ${item.myTotalContribution}")
            if (item.myTodayContribution > 0) append(" · 今日 +${item.myTodayContribution}")
            if (item.myContributionRank > 0) append(" · 贡献榜第${item.myContributionRank}名")
        }

        //贡献榜前3头像
        val container = binding.llTop3
        container.removeAllViews()
        val size = (resources.displayMetrics.density * 26).toInt()
        val lp = LinearLayout.LayoutParams(size, size).apply {
            marginEnd = (resources.displayMetrics.density * 4).toInt()
        }
        item.top3ContributionUserAvatar.orEmpty().forEach { avatar ->
            val iv = android.widget.ImageView(context)
            iv.layoutParams = lp
            iv.loadHead(avatar)
            container.addView(iv)
        }
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack, binding.tvRecords, binding.tvDonate, binding.tvLuckyBag
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvRecords -> {
                val dialog = recordDialog ?: DonateRecordDialog(requireContext(), mViewModel)
                    .also { recordDialog = it }
                dialog.show()
            }
            binding.tvDonate -> {
                val buddha = current ?: return
                val dialog = coinDialog ?: DonateCoinDialog(requireContext()).also {
                    it.onDonate = { coins -> mViewModel.donateBuddha(buddha.id, coins) }
                    coinDialog = it
                }
                dialog.render(buddha.buddhaName)
                dialog.show()
            }
            binding.tvLuckyBag -> {
                current?.let { mViewModel.gainLuckyBag(it.id) }
            }
        }
    }
}

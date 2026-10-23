package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.SignInData
import com.fingertip.baselib.top.TopDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogSignInBinding

/**
 * 签到
 * 只负责展示和点击，数据由外面的 SignInVM 拉，签到成功再刷新一次
 */
class SignInDialog(context: Context) : TopDialog(context) {

    companion object {
        //服务端「今天已经签到过了」
        private const val CODE_TODAY_SIGNED = -24
    }

    private lateinit var binding: DialogSignInBinding

    //点签到按钮，实际请求交给宿主页面
    var onSignIn: (() -> Unit)? = null

    //拉取失败后点「重新加载」
    var onReload: (() -> Unit)? = null

    private var data: SignInData? = null

    //请求还没回来，防止连点
    private var signing = false

    //上次拉取失败，此时按钮是「重新加载」
    private var loadFailed = false

    override fun attachLayoutRes(): Int = R.layout.dialog_sign_in

    override fun onViewInit(view: View?) {
        if (view == null) return
        binding = DialogSignInBinding.bind(view)
        binding.ivClose.setOnClickListener { dismiss() }
        binding.clShadow.setOnClickListener {
            if (loadFailed) {
                showLoading()
                onReload?.invoke()
                return@setOnClickListener
            }
            if (signing || data?.hasSignIn == true) return@setOnClickListener
            signing = true
            renderButton()
            onSignIn?.invoke()
        }
        showLoading()
    }

    /**
     * 打开后、数据没回来之前的状态
     */
    fun showLoading() {
        loadFailed = false
        signing = false
        data = null
        binding.tvContinuousDays.text = "-"
        binding.tvTotalDays.text = "正在加载签到信息…"
        binding.llDaily.visibility = View.GONE
        binding.llContinuous.visibility = View.GONE
        binding.tvSignIn.text = "加载中…"
        binding.clShadow.alpha = 0.5f
    }

    /**
     * 拉签到信息失败
     */
    fun showLoadError() {
        loadFailed = true
        signing = false
        data = null
        binding.tvContinuousDays.text = "-"
        binding.tvTotalDays.text = "签到信息加载失败，请稍后再试"
        binding.llDaily.visibility = View.GONE
        binding.llContinuous.visibility = View.GONE
        binding.tvSignIn.text = "重新加载"
        binding.clShadow.alpha = 1f
    }

    /**
     * 渲染签到信息
     */
    fun render(signInData: SignInData?) {
        if (signInData == null) {
            showLoadError()
            return
        }
        loadFailed = false
        signing = false
        data = signInData

        binding.tvContinuousDays.text = signInData.continuousDays.toString()
        binding.tvTotalDays.text = "累计签到 ${signInData.totalSignDays} 天"

        renderRewardCards(signInData)
        renderContinuous(signInData)
        renderButton()
    }

    /**
     * 每次签到都给的基础奖励。接口返几条就显示几张卡片，多出来的忽略
     */
    private fun renderRewardCards(signInData: SignInData) {
        val rewards = signInData.signInRewards.orEmpty()
        val cards = listOf(
            Triple(binding.llReward1, binding.tvRewardName1, binding.tvRewardValue1),
            Triple(binding.llReward2, binding.tvRewardName2, binding.tvRewardValue2)
        )
        var shown = 0
        cards.forEachIndexed { index, card ->
            val item = rewards.getOrNull(index)
            if (item == null) {
                card.first.visibility = View.GONE
            } else {
                card.first.visibility = View.VISIBLE
                card.second.text = item.rewardName
                card.third.text = "+${item.rewardValue}"
                shown++
            }
        }
        binding.llDaily.visibility = if (shown == 0) View.GONE else View.VISIBLE
    }

    /**
     * 下一档连续签到奖励。所有档位都拿过时接口给 null，整块藏起来
     */
    private fun renderContinuous(signInData: SignInData) {
        val next = signInData.nextContinuousReward
        if (next == null) {
            binding.llContinuous.visibility = View.GONE
            return
        }
        binding.llContinuous.visibility = View.VISIBLE
        binding.tvContinuousTitle.text = "连续签到 ${next.continuousDays} 天"
        binding.tvContinuousRemain.text = "还差 ${next.remainDays} 天"
        binding.tvContinuousDesc.text = "再签 ${next.remainDays} 天，额外得 ${next.rewardText()}"
        binding.pbContinuous.progress = next.progress(signInData.continuousDays)
    }

    private fun renderButton() {
        val signInData = data
        when {
            signInData == null -> {
                binding.tvSignIn.text = if (signing) "签到中…" else "签到"
                binding.clShadow.alpha = 0.5f
            }
            signInData.hasSignIn -> {
                binding.tvSignIn.text = "今日已签到"
                binding.clShadow.alpha = 0.5f
            }
            else -> {
                binding.tvSignIn.text = "签到领取 ${signInData.basicRewardText()}"
                binding.clShadow.alpha = 1f
            }
        }
    }

    /**
     * 签到成功。具体新数据由宿主页面重新拉，这里只给个反馈。
     * 先把本地状态置成已签到，避免新数据回来之前按钮还能再点一次
     */
    fun onSignInSuccess() {
        signing = false
        data?.hasSignIn = true
        ToastUtil.showMessage("签到成功")
        renderButton()
    }

    /**
     * 签到失败，-24 是今天已经签过，其他情况提示重试
     */
    fun onSignInFailed(errorCode: Int) {
        signing = false
        if (errorCode == CODE_TODAY_SIGNED) {
            data?.hasSignIn = true
            ToastUtil.showMessage("今天已经签到过了")
        } else {
            ToastUtil.showMessage("签到失败，请稍后再试")
        }
        renderButton()
    }
}

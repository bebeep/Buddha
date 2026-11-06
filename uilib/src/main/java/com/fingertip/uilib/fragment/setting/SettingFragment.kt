package com.fingertip.uilib.fragment.setting

import android.content.Intent
import android.view.View
import com.blankj.utilcode.util.AppUtils
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.activity.LoginRegisterActivity
import com.fingertip.uilib.databinding.FragmentSettingBinding
import com.fingertip.uilib.dialog.ConfirmDialog
import com.fingertip.uilib.pay.IapBridge
import com.fingertip.uilib.viewmodel.SettingVM

/**
 * 设置
 *
 * 三块：账号（退出 / 注销）、隐私与合规（三份本地法律文本）、关于（版本号）。
 *
 * 注销账号这条必须保留：Google Play 的账号删除政策要求「应用内可发起删除」，
 * 只在隐私政策里写个邮箱是过不了审的。所以这里做成两次确认 + 服务端真删数据。
 *
 * 退出登录与注销的区别要在文案上写清楚：
 * 退出只是清 session，装扮和功德币都还在；注销是删账号，数据不可恢复。
 */
class SettingFragment : TopPmFragment<SettingVM>() {

    override fun layoutId() = R.layout.fragment_setting

    override fun initVM(): SettingVM = provideVM()

    private val binding get() = mBinding as FragmentSettingBinding

    //恢复购买正在跑，避免连点重复请求
    private var restoring = false

    override fun initShiTu() {
        binding.tvVersion.text = AppUtils.getAppVersionName()
        //游客号才展示绑定引导；绑定过 Google/Apple 或邮箱登录的都不显示
        val guest = GlobalConfig.isGuestAccount
        binding.llBindAccount.visibility = if (guest) View.VISIBLE else View.GONE
        binding.divBindAccount.visibility = if (guest) View.VISIBLE else View.GONE
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.logoutResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                clearLocalSession()
                ToastUtil.showMessage(getString(R.string.setting_logout_done))
                pop()
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.setting_logout_fail))
            }
        }

        mViewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                clearLocalSession()
                ToastUtil.showMessage(getString(R.string.setting_delete_done))
                //账号已删，回到登录页并清掉任务栈，避免用户退回到已注销账号的主界面
                startActivity(
                    Intent(requireContext(), LoginRegisterActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
                activity?.finish()
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.setting_delete_fail))
            }
        }

        mViewModel.restoreResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            restoring = false
            if (result.success) {
                val count = result.data ?: 0
                ToastUtil.showMessage(
                    if (count > 0) getString(R.string.setting_restore_done, count)
                    else getString(R.string.setting_restore_none)
                )
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.setting_restore_fail))
            }
        }
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack,
        binding.llBindAccount,
        binding.llBlockList,
        binding.llRestore,
        binding.llLogout,
        binding.llDelete,
        binding.llPrivacy,
        binding.llTerms,
        binding.llLicense
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()

            //游客号绑定引导：去登录页绑 Google/Apple，账号不换、数据延续。
            //清栈重进：绑定成功后走 ContainerActivity，和冷启动登录后的栈保持一致
            binding.llBindAccount -> {
                if (!GlobalConfig.isGuestAccount) return@onSingleClick
                startActivity(
                    Intent(requireContext(), LoginRegisterActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
            }

            //拉黑名单管理。放在账号组最前，是因为它属于"用户自己控制能看到什么"这类设置
            binding.llBlockList -> {
                if (GlobalConfig.session.isBlank()) {
                    ToastUtil.showMessage(getString(R.string.setting_not_login))
                    return
                }
                (parentFragment as? com.fingertip.uilib.fragment.MainFragment)
                    ?.start(BlockListFragment())
            }

            //恢复购买：换设备/重装后把已买的装扮找回来
            binding.llRestore -> {
                if (restoring) return
                if (GlobalConfig.session.isBlank()) {
                    ToastUtil.showMessage(getString(R.string.setting_restore_not_login))
                    return
                }
                restoring = true
                IapBridge.restorePurchases(activity) { tokens ->
                    if (!isAdded) return@restorePurchases
                    if (tokens.isEmpty()) {
                        //平台侧查不到交易号，说明确实没买过（或支付通道未接入），直接给结论
                        restoring = false
                        ToastUtil.showMessage(getString(R.string.setting_restore_none))
                    } else {
                        mViewModel.restorePurchases(tokens)
                    }
                }
            }

            binding.llLogout -> {
                if (GlobalConfig.session.isBlank()) {
                    ToastUtil.showMessage(getString(R.string.setting_not_login))
                    return
                }
                ConfirmDialog(
                    requireContext(),
                    title = getString(R.string.setting_logout_title),
                    message = getString(R.string.setting_logout_msg),
                    confirmText = getString(R.string.setting_logout),
                    onConfirm = { mViewModel.logout() }
                ).show()
            }

            binding.llDelete -> {
                if (GlobalConfig.session.isBlank()) {
                    ToastUtil.showMessage(getString(R.string.setting_not_login))
                    return
                }
                ConfirmDialog(
                    requireContext(),
                    title = getString(R.string.setting_delete_title),
                    message = getString(R.string.setting_delete_msg),
                    confirmText = getString(R.string.setting_delete_confirm),
                    onConfirm = { mViewModel.deleteAccount() }
                ).show()
            }

            binding.llPrivacy -> pushDoc(getString(R.string.setting_privacy), LegalDocFragment.ASSET_PRIVACY)
            binding.llTerms -> pushDoc(getString(R.string.setting_terms), LegalDocFragment.ASSET_TERMS)
            binding.llLicense -> pushDoc(getString(R.string.setting_license), LegalDocFragment.ASSET_LICENSE)
        }
    }

    private fun pushDoc(title: String, asset: String) {
        (parentFragment as? com.fingertip.uilib.fragment.MainFragment)
            ?.start(LegalDocFragment.newInstance(title, asset))
    }

    /**
     * 清本地登录态。userData 的 setter 会顺带把 session 置空，
     * 所以先置 userData 再显式清一次 session，防止 Setter 逻辑变动后漏清。
     */
    private fun clearLocalSession() {
        // 先清设备档案：旧账号不该继续被提醒任务命中（IM 已 logout，离线推送自然停）
        com.fingertip.uilib.push.DeviceProfile.clear()
        // IM 掉线，清前台通知与角标
        com.fingertip.uilib.im.ImManager.logout()
        GlobalConfig.userData = null
        GlobalConfig.session = ""
        // session 必须一起清掉，否则下次冷启动会拿已失效的 session 白跑一次续期
        com.blankj.utilcode.util.SPUtils.getInstance(com.fingertip.baselib.constant.SPConstant.SP_ACCOUNT)
            .put(com.fingertip.baselib.constant.SPConstant.SP_SESSION, "")
    }
}

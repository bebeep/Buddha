package com.fingertip.uilib.fragment.setting

import android.util.Patterns
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentAccountBindBinding
import com.fingertip.uilib.login.GoogleLoginConfig
import com.fingertip.uilib.login.GoogleLoginHelper
import com.fingertip.uilib.viewmodel.AccountBindVM

/**
 * 账号绑定（海外版）
 *
 * 已登录用户在这里绑定 / 管理：邮箱（主）、Google。
 * - 邮箱：绑成功后本地立即刷新 email，并建议填写密码（setPassword），否则绑了邮箱也无法用「邮箱 + 密码」登录。
 * - Google：绑成功后本地仅标记已绑（真实 sub 由后端保存，下次登录 / 续期回拉）。
 * Apple 在 Android 端无原生登录，本页不提供。
 */
class AccountBindFragment : TopPmFragment<AccountBindVM>() {

    override fun layoutId() = R.layout.fragment_account_bind

    override fun initVM(): AccountBindVM = provideVM()

    private val binding get() = mBinding as FragmentAccountBindBinding

    private lateinit var googleHelper: GoogleLoginHelper

    // 当前绑定操作是否为 Google，用于绑成功后刷新对应状态
    private var pendingIsGoogle = false

    override fun initShiTu() {
        googleHelper = GoogleLoginHelper(requireContext())
        // Google 未配置就隐藏整个 Google 绑定项
        binding.llGoogle.visibility = if (GoogleLoginConfig.isConfigured) View.VISIBLE else View.GONE
        refreshUi()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.bindResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                if (pendingIsGoogle) {
                    // 本地仅标记已绑 Google；真实 sub 会在下次登录 / 续期时被后端回拉覆盖
                    saveGoogle("bound")
                    ToastUtil.showMessage(getString(R.string.account_bind_google_ok))
                } else {
                    val email = binding.etEmail.text.toString().trim().lowercase()
                    if (email.isNotEmpty()) saveEmail(email)
                    ToastUtil.showMessage(getString(R.string.account_bind_email_ok))
                    // 绑邮箱时若填了密码（>=8 位），紧接着设置密码，使邮箱可用于登录
                    val pwd = binding.etPwd.text.toString()
                    if (pwd.length >= 8) mViewModel.setPassword(pwd)
                }
                pendingIsGoogle = false
                refreshUi()
            }
        }

        mViewModel.setPwdResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                binding.etPwd.setText("")
                ToastUtil.showMessage(getString(R.string.account_bind_pwd_ok))
            }
        }
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack,
        binding.btnEmail,
        binding.btnGoogle
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.btnEmail -> doBindEmail()
            binding.btnGoogle -> doBindGoogle()
        }
    }

    private fun refreshUi() {
        val u = GlobalConfig.userData
        val email = u?.email
        binding.tvEmailStatus.text = if (!email.isNullOrBlank())
            getString(R.string.account_bind_email_bound, email)
        else
            getString(R.string.account_bind_email_unbound)
        binding.btnEmail.text = if (!email.isNullOrBlank())
            getString(R.string.account_bind_email_update)
        else
            getString(R.string.account_bind_email_btn)

        val googleBound = !u?.googleId.isNullOrBlank()
        binding.tvGoogleStatus.text = if (googleBound)
            getString(R.string.account_bind_google_bound)
        else
            getString(R.string.account_bind_google_unbound)
        binding.btnGoogle.text = if (googleBound)
            getString(R.string.account_bind_google_update)
        else
            getString(R.string.account_bind_google_btn)
    }

    private fun saveEmail(email: String) {
        val u = GlobalConfig.userData ?: return
        u.email = email
        GlobalConfig.userData = u // 触发 setter 持久化到 SP
    }

    private fun saveGoogle(mark: String) {
        val u = GlobalConfig.userData ?: return
        u.googleId = mark
        GlobalConfig.userData = u
    }

    private fun doBindEmail() {
        val email = binding.etEmail.text.toString().trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ToastUtil.showMessage(getString(R.string.account_bind_email_invalid))
            return
        }
        val pwd = binding.etPwd.text.toString()
        if (pwd.isNotEmpty() && pwd.length < 8) {
            ToastUtil.showMessage(getString(R.string.account_bind_pwd_short))
            return
        }
        pendingIsGoogle = false
        mViewModel.bindThird(1, email)
    }

    private fun doBindGoogle() {
        pendingIsGoogle = true
        googleHelper.signIn(
            scope = lifecycleScope,
            onSuccess = { idToken ->
                mViewModel.bindThird(2, idToken = idToken)
            },
            onError = { msg ->
                pendingIsGoogle = false
                ToastUtil.showMessage(msg)
            }
        )
    }
}

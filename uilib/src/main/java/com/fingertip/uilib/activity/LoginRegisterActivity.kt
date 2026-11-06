package com.fingertip.uilib.activity

import com.fingertip.baselib.R as BaseR
import com.fingertip.baselib.top.TopApplication
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.top.TopVMActivity
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ActivityLoginRegisterBinding
import com.fingertip.uilib.login.GoogleLoginConfig
import com.fingertip.uilib.login.GoogleLoginHelper
import com.fingertip.uilib.push.DeviceProfile
import com.fingertip.uilib.viewmodel.LoginRegisterVM

/**
 * 登录 / 注册（海外版）
 *
 * 一屏两态：登录 / 注册，靠 tv_switch_mode 切换，不另开页面——
 * 海外用户对「同一张表单切换登录/注册」的接受度更高，也少一次跳转。
 *
 * 手机号、微信登录已移除：
 *   - 手机号：海外短信到达率差且成本高，实际转化不如 Google 一键登录
 *   - 微信：海外渗透率低
 *
 * 游客登录保留：服务端按设备号创建/找回游客号，享正式账号全部功能；
 * 之后用 Google/Apple 登录时服务端把第三方凭证直接绑到该游客号上，账号不换、数据不丢。
 */
class LoginRegisterActivity : TopVMActivity<LoginRegisterVM>() {

    companion object {
        /** 与 App Store / Google Play 对账号安全的最低要求对齐 */
        private const val MIN_PASSWORD_LENGTH = 8

        /** 邮箱查重防抖：边输边查会把接口打成筛子 */
        private const val EMAIL_CHECK_DELAY = 600L

        /** 服务端 ReturnCode.EMAIL_EXIST，用来区分「邮箱被注册」与「网络失败」 */
        private const val CODE_EMAIL_EXIST = -47
    }

    private lateinit var binding: ActivityLoginRegisterBinding
    private lateinit var googleHelper: GoogleLoginHelper

    private val mainHandler = Handler(Looper.getMainLooper())
    private var emailCheckRunnable: Runnable? = null

    private var isRegisterMode = false

    override fun initVM(): LoginRegisterVM = provideVM()

    override fun isFullTopBar() = true

    override fun listOfClickView(): List<View> = listOf(
        binding.tvAction,
        binding.tvSwitchMode,
        binding.tvTerms,
        binding.tvPrivacy,
        binding.llGoogle,
        binding.tvGuest
    )

    override fun initShiTu() {
        binding = ActivityLoginRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        googleHelper = GoogleLoginHelper(this)

        // 没配 Google client id 就整块隐藏：宁可少一个入口，也不给一个点了必然失败的按钮
        binding.llGoogle.visibility = if (GoogleLoginConfig.isConfigured) View.VISIBLE else View.GONE

        val lastEmail = SPUtils.getInstance(SPConstant.SP_ACCOUNT)
            .getString(SPConstant.SP_LAST_EMAIL)
        if (lastEmail.isNotEmpty()) {
            binding.etEmail.setText(lastEmail)
            binding.etEmail.setSelection(binding.etEmail.text.length)
        }

        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = scheduleEmailCheck()
        })

        applyMode()
    }

    override fun initObserver() {
        mViewModel.loginResult.observe(this) { result ->
            if (result.success && result.data != null) {
                enterMain()
            }
        }
        mViewModel.registerResult.observe(this) { result ->
            if (result.success && result.data != null) {
                enterMain()
            }
        }
        mViewModel.checkEmailResult.observe(this) { result ->
            if (result.success) {
                binding.tvEmailTip.visibility = View.VISIBLE
                binding.tvEmailTip.setText(R.string.login_email_ok)
                binding.tvEmailTip.setTextColor(TopApplication.instance.getColor(BaseR.color.c_gold))
            } else if (result.errorCode == CODE_EMAIL_EXIST) {
                binding.tvEmailTip.visibility = View.VISIBLE
                binding.tvEmailTip.setText(R.string.login_email_exist)
                binding.tvEmailTip.setTextColor(TopApplication.instance.getColor(BaseR.color.c_d98a6a))
            } else {
                // 网络抖动等情况不误报「已注册」，直接收起提示
                binding.tvEmailTip.visibility = View.GONE
            }
        }
    }

    override fun respondOnceClick(v: View?) {
        super.respondOnceClick(v)
        when (v) {
            binding.tvAction -> submit()

            binding.tvSwitchMode -> {
                isRegisterMode = !isRegisterMode
                applyMode()
            }

            binding.llGoogle -> signInWithGoogle()

            //游客进入：不用填任何东西，服务端按设备号建/找回账号
            binding.tvGuest -> mViewModel.guestLogin()

            binding.tvTerms -> openLegal(
                getString(R.string.login_terms),
                "file:///android_asset/legal/terms.html"
            )

            binding.tvPrivacy -> openLegal(
                getString(R.string.login_privacy),
                "file:///android_asset/legal/privacy.html"
            )
        }
    }

    private fun applyMode() {
        binding.tvAction.setText(
            if (isRegisterMode) R.string.login_action_register else R.string.login_action_login
        )
        binding.tvSwitchMode.setText(
            if (isRegisterMode) R.string.login_to_login else R.string.login_to_register
        )
        binding.etPasswordAgain.visibility = if (isRegisterMode) View.VISIBLE else View.GONE
        binding.tvEmailTip.visibility = View.GONE
    }

    private fun submit() {
        val email = binding.etEmail.text.toString().trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ToastUtil.showMessage(getString(R.string.login_email_invalid))
            return
        }

        val pwd = binding.etPassword.text.toString()
        if (pwd.length < MIN_PASSWORD_LENGTH) {
            ToastUtil.showMessage(getString(R.string.login_pwd_short))
            return
        }

        if (isRegisterMode) {
            if (pwd != binding.etPasswordAgain.text.toString()) {
                ToastUtil.showMessage(getString(R.string.login_pwd_not_match))
                return
            }
            mViewModel.register(email, pwd)
        } else {
            mViewModel.loginByEmail(email, pwd)
        }
    }

    private fun scheduleEmailCheck() {
        emailCheckRunnable?.let { mainHandler.removeCallbacks(it) }

        if (!isRegisterMode) {
            binding.tvEmailTip.visibility = View.GONE
            return
        }
        val email = binding.etEmail.text.toString().trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tvEmailTip.visibility = View.GONE
            return
        }

        val runnable = Runnable { mViewModel.checkEmail(email) }
        emailCheckRunnable = runnable
        mainHandler.postDelayed(runnable, EMAIL_CHECK_DELAY)
    }

    private fun signInWithGoogle() {
        googleHelper.signIn(
            scope = lifecycleScope,
            onSuccess = { idToken ->
                mViewModel.loginByThird(LoginRegisterVM.TYPE_GOOGLE, idToken)
            },
            onCancel = {
                // 用户主动关掉账号选择，什么也不做
            },
            onError = { msg ->
                val text = if (msg.contains("没有可用")) {
                    getString(R.string.login_google_no_account)
                } else {
                    getString(R.string.login_google_fail)
                }
                ToastUtil.showMessage(text)
            }
        )
    }

    private fun openLegal(title: String, url: String) {
        CommonWebViewActivity.start(this, title, url)
    }

    private fun enterMain() {
        // 登录完成即上报设备档案（FCM token + 时区，提醒与离线推送的数据源）
        DeviceProfile.report()
        DeviceProfile.requestNotificationPermission(this)
        // IM 登录（换设备同步：登录后云端自动同步会话与漫游消息）
        com.fingertip.uilib.im.ImManager.init(this)
        com.fingertip.uilib.im.ImManager.login(GlobalConfig.userData?.accountId ?: 0)
        startActivity(Intent(this, ContainerActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        emailCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}

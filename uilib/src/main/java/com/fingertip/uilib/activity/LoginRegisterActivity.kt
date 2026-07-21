package com.fingertip.uilib.activity

import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.Toast
import com.fingertip.baselib.top.TopVMActivity
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ActivityLoginRegisterBinding
import com.fingertip.uilib.viewmodel.StartUpVM
import androidx.core.graphics.toColorInt
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.constant.SPConstant
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.viewmodel.LoginRegisterVM

/**
 * 登录注册
 */
class LoginRegisterActivity : TopVMActivity<LoginRegisterVM>() {

    private var isVerifyCodeMode = true
    private lateinit var binding: ActivityLoginRegisterBinding

    override fun initVM(): LoginRegisterVM = provideVM()


    override fun isFullTopBar() = true

    override fun listOfClickView(): List<View> = listOf(binding.tvTabVerify,binding.tvTabPassword,binding.tvGetCode,binding.tvLogin)


    override fun initShiTu() {
        binding = ActivityLoginRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun respondOnceClick(v: View?) {
        super.respondOnceClick(v)
        when (v?.id) {
            R.id.tv_tab_verify -> {
                isVerifyCodeMode = true
                binding.tvTabVerify.setBackgroundResource(R.drawable.bg_login_tab_selected)
                binding.tvTabVerify.setTextColor(resources.getColor(android.R.color.white, theme))
                binding.tvTabPassword.setBackgroundResource(0)
                binding.tvTabPassword.setTextColor("#999999".toColorInt())
                binding.llVerifyInput.visibility = View.VISIBLE
                binding.tvVerifyLabel.visibility = View.VISIBLE
                binding.llPasswordInput.visibility = View.GONE
                binding.tvPasswordLabel.visibility = View.GONE
                binding.tvForgotPassword.visibility = View.VISIBLE
                binding.tvGetCode.visibility = View.VISIBLE
            }
            R.id.tv_tab_password -> {
                isVerifyCodeMode = false
                binding.tvTabPassword.setBackgroundResource(R.drawable.bg_login_tab_selected)
                binding.tvTabPassword.setTextColor(resources.getColor(android.R.color.white, theme))
                binding.tvTabVerify.setBackgroundResource(0)
                binding.tvTabVerify.setTextColor("#999999".toColorInt())
                binding.llVerifyInput.visibility = View.GONE
                binding.tvVerifyLabel.visibility = View.GONE
                binding.llPasswordInput.visibility = View.VISIBLE
                binding.tvPasswordLabel.visibility = View.VISIBLE
                binding.tvForgotPassword.visibility = View.VISIBLE
                binding.tvGetCode.visibility = View.GONE
            }
            R.id.tv_get_code -> {
                // 获取验证码
                val phone = binding.etPhone.text.toString().trim()
                if (phone.isEmpty()) {
                    ToastUtil.showMessage("请输入手机号" )
                    return
                }
                ToastUtil.showMessage( "验证码已发送")
            }
            R.id.tv_login -> {
                // 登录
                val phone = binding.etPhone.text.toString().trim()
                if (phone.isEmpty()) {
                    ToastUtil.showMessage("请输入手机号" )
                    return
                }
                if (isVerifyCodeMode) {
                    val code = binding.etVerifyCode.text.toString().trim()
                    if (code.isEmpty()) {
                        ToastUtil.showMessage("请输入验证码" )
                        return
                    }
                    mViewModel.login(phone, code)
                } else {
                    val password = binding.etPassword.text.toString().trim()
                    if (password.isEmpty()) {
                        ToastUtil.showMessage("请输入密码" )
                        return
                    }
                    mViewModel.login(phone, password)
                }
            }
        }
    }

    override fun initObserver() {
        mViewModel.loginResult.observe(this) {
            if (it.success && it.data != null) {
                // 跳转到主页
                GlobalConfig.userData = it.data
                startActivity(Intent(this,ContainerActivity::class.java))
                finish()
            }
        }
    }
}
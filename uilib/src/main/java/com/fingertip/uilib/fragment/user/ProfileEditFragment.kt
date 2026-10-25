package com.fingertip.uilib.fragment.user

import android.view.View
import android.widget.RadioGroup
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentProfileEditBinding
import com.fingertip.uilib.viewmodel.UserVM

/**
 * 编辑个人资料，对应 account 的 modifyProfile
 * 昵称/签名/性别；头像封面走 OSS 上传流程，暂未开放
 */
class ProfileEditFragment : TopPmFragment<UserVM>() {

    companion object {
        private const val TAG = "ProfileEditFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_profile_edit

    override fun initVM(): UserVM = provideVM()

    private val binding get() = mBinding as FragmentProfileEditBinding

    private var currentGender = 0

    override fun initShiTu() {
        val user = GlobalConfig.userData
        binding.etName.setText(user?.userName)
        binding.etMood.setText(user?.mood)
        currentGender = user?.gender ?: 0
        when (currentGender) {
            1 -> binding.rbMale.isChecked = true
            2 -> binding.rbFemale.isChecked = true
            else -> binding.rbSecret.isChecked = true
        }
        binding.rgGender.setOnCheckedChangeListener { _, checkedId ->
            currentGender = when (checkedId) {
                R.id.rb_male -> 1
                R.id.rb_female -> 2
                else -> 0
            }
        }
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.modifyResult.observe(viewLifecycleOwner) { result ->
            binding.tvSave.isEnabled = true
            if (result.success) {
                ToastUtil.showMessage("保存成功")
                //同步本地并刷新个人中心
                GlobalConfig.userData?.apply {
                    userName = binding.etName.text?.toString()?.trim() ?: userName
                    mood = binding.etMood.text?.toString()?.trim() ?: mood
                    gender = currentGender
                }
                pop()
            } else {
                log(TAG, "修改资料失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "保存失败，请稍后再试")
            }
        }
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack, binding.tvSave)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvSave -> {
                val name = binding.etName.text?.toString()?.trim().orEmpty()
                val mood = binding.etMood.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    ToastUtil.showMessage("昵称不能为空")
                    return
                }
                binding.tvSave.isEnabled = false
                mViewModel.modifyProfile(
                    userName = name,
                    avatar = null,
                    mood = mood.takeIf { it.isNotEmpty() },
                    gender = currentGender,
                    cover = null
                )
            }
        }
    }
}

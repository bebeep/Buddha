package com.fingertip.uilib.fragment.user

import android.view.View
import android.widget.RadioGroup
import androidx.lifecycle.ViewModelProvider
import com.fingertip.baselib.bean.ShopDressConfig
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentProfileEditBinding
import com.fingertip.uilib.fragment.shop.MyDressFragment
import com.fingertip.uilib.viewmodel.ShopVM
import com.fingertip.uilib.viewmodel.UserVM

/**
 * 编辑个人资料（暗调禅金），对应 account 的 modifyProfile。
 *
 * 昵称/签名保存走服务端敏感词审核（scene 10/11），客户端只做长度与空值校验。
 *
 * 昵称 3D 徽章开关：拥有昵称3D装扮（dressType=4，IAP 购买/里程碑奖励/运营赠送）才显示，
 * 开关即佩戴/摘下（dress/wear），与「我的装扮」页共用同一份服务端佩戴态，切完立即生效。
 * 头像换图走 OSS 上传流程未接，头像位与「佩戴装扮」小字先作为我的装扮入口。
 */
class ProfileEditFragment : TopPmFragment<UserVM>() {

    companion object {
        private const val TAG = "ProfileEditFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_profile_edit

    override fun initVM(): UserVM = provideVM()

    private val binding get() = mBinding as FragmentProfileEditBinding

    /** 装扮数据源：3D 开关需要 dress/getMy 判断拥有与佩戴态，单独挂一个 VM，不动 UserVM 职责 */
    private val shopVM by lazy { ViewModelProvider(this)[ShopVM::class.java] }

    private var currentGender = 0

    /** 当前拥有的昵称3D装扮 id，0 = 未拥有（开关隐藏） */
    private var name3dDressId = 0

    /** 程序化 setChecked 期间屏蔽监听，避免把回显当成用户操作发起佩戴请求 */
    private var suppressSwitch = false

    override fun initShiTu() {
        val user = GlobalConfig.userData
        binding.ivAvatar.loadHead(user?.avatar)
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

        binding.switchName3d.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitch || name3dDressId <= 0) return@setOnCheckedChangeListener
            shopVM.wear(name3dDressId, isChecked)
        }

        shopVM.loadMyDress()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.modifyResult.observe(viewLifecycleOwner) { result ->
            binding.tvSave.isEnabled = true
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.profile_edit_save_ok))
                //同步本地并刷新个人中心
                GlobalConfig.userData?.apply {
                    userName = binding.etName.text?.toString()?.trim() ?: userName
                    mood = binding.etMood.text?.toString()?.trim() ?: mood
                    gender = currentGender
                }
                pop()
            } else {
                log(TAG, "修改资料失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.profile_edit_save_fail))
            }
        }

        //装扮数据回来：判断是否拥有昵称3D，决定开关显示与回显状态
        shopVM.myResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (!result.success) {
                log(TAG, "我的装扮获取失败：${result.errorCode} ${result.errorMsg}")
                return@observe
            }
            val data = result.data ?: return@observe
            val owned = data.dresses.orEmpty()
                .firstOrNull { it.dressType == ShopDressConfig.TYPE_NAME3D && it.isDel == 0 }
            name3dDressId = owned?.dressId ?: 0
            if (name3dDressId <= 0) {
                binding.llName3d.visibility = View.GONE
                return@observe
            }
            binding.llName3d.visibility = View.VISIBLE
            suppressSwitch = true
            binding.switchName3d.isChecked = data.wearingId(ShopDressConfig.TYPE_NAME3D) == name3dDressId
            suppressSwitch = false
        }

        //佩戴结果：失败回滚开关状态，成功以服务端为准重拉
        shopVM.wearResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.profile_edit_3d_synced))
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.profile_edit_save_fail))
            }
            shopVM.loadMyDress()
        }
    }

    override fun getClickViews(): List<View> =
        listOf(binding.ivBack, binding.tvSave, binding.ivAvatar, binding.tvWearHint)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()

            //头像与佩戴装扮小字都进我的装扮：那里能换灯具/佛龛/头像框/昵称3D
            binding.ivAvatar, binding.tvWearHint ->
                (parentFragment as? com.fingertip.uilib.fragment.MainFragment)?.start(MyDressFragment())

            binding.tvSave -> {
                val name = binding.etName.text?.toString()?.trim().orEmpty()
                val mood = binding.etMood.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    ToastUtil.showMessage(getString(R.string.profile_edit_name_empty))
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

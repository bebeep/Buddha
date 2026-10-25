package com.fingertip.uilib.fragment.user

import android.os.Bundle
import android.view.View
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentUserInfoBinding
import com.fingertip.uilib.viewmodel.UserVM

/**
 * 用户信息页（他人主页），对应原型「用户信息」与 account 的 getUserInfo/follow/unFollow
 *
 * - 展示头像/昵称/等级/签名/关注/佛友数
 * - 关注/取消关注按钮；功德捐赠数在他人主页服务端会置0，仅本人可见
 * - TA的感悟：后端暂无按用户的动态查询接口，先展示空态，待接口补充后接入
 */
class UserInfoFragment : TopPmFragment<UserVM>() {

    companion object {
        private const val ARG_ACCOUNT_ID = "arg_account_id"
        private const val TAG = "UserInfoFragment"

        fun newInstance(accountId: Int): UserInfoFragment {
            return UserInfoFragment().apply {
                arguments = Bundle().apply { putInt(ARG_ACCOUNT_ID, accountId) }
            }
        }
    }

    override fun layoutId(): Int = R.layout.fragment_user_info

    override fun initVM(): UserVM = provideVM()

    private val binding get() = mBinding as FragmentUserInfoBinding

    private var accountId = 0
    private var isSelf = false
    private var followed = false

    override fun initShiTu() {
        accountId = arguments?.getInt(ARG_ACCOUNT_ID, 0) ?: 0
        isSelf = accountId == (GlobalConfig.userData?.accountId ?: -1)
        binding.tvFollow.visibility = if (isSelf) View.GONE else View.VISIBLE

        //后端暂无按用户的动态查询接口，先显示空态
        binding.rcMoments.visibility = View.GONE
        binding.tvMomentEmpty.visibility = View.VISIBLE

        mViewModel.loadUserInfo(accountId)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.userInfoResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                render(result.data ?: return@observe)
            } else {
                log(TAG, "用户信息获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败")
            }
        }

        mViewModel.followResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                followed = true
                binding.tvFollow.text = "已关注"
                ToastUtil.showMessage("关注成功")
            } else {
                ToastUtil.showMessage(result.errorMsg ?: "关注失败")
            }
        }

        mViewModel.unFollowResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                followed = false
                binding.tvFollow.text = "关注"
                ToastUtil.showMessage("已取消关注")
            } else {
                ToastUtil.showMessage(result.errorMsg ?: "操作失败")
            }
        }
    }

    private fun render(user: PersonData) {
        binding.ivAvatar.loadHead(user.avatar)
        binding.tvName.text = user.userName
        binding.tvLevel.text = "lv${user.userLevel}"
        binding.tvMood.text = user.mood
        binding.tvDisplayId.text = "ID:${user.displayAccountId ?: ""}"
        binding.tvFollowings.text = "${user.followingCount ?: 0}"
        binding.tvFollowers.text = "${user.followerCount ?: 0}"
        binding.tvDonateTotal.text = "${user.coin}"

        //功德捐赠数他人主页服务端置0，隐藏行避免误读
        binding.tvDonateTotal.visibility = if (isSelf) View.VISIBLE else View.GONE
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack, binding.tvFollow)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvFollow -> {
                if (isSelf) return
                if (followed) mViewModel.unFollow(accountId) else mViewModel.follow(accountId)
            }
        }
    }
}

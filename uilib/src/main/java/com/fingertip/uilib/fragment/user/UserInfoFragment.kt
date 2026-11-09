package com.fingertip.uilib.fragment.user

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.event_bus.EventBusProxy
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MomentAdapter
import com.fingertip.uilib.databinding.FragmentUserInfoBinding
import com.fingertip.uilib.dialog.ActionSheetDialog
import com.fingertip.uilib.dialog.ConfirmDialog
import com.fingertip.uilib.dialog.ReportDialog
import com.fingertip.uilib.fragment.moment.MomentDetailsFragment
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.MomentVM
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

    /** 当前主页用户（render 后缓存，发消息要带昵称/头像） */
    private var peerUser: PersonData? = null

    //当前是否已拉黑对方。由 /userTool/getBlockList 反查得到，
    //后端没有再单独提供 isBlocked 接口，列表通常就几条，本地遍历足够
    private var blocked = false

    //TA 的感悟：动态列表与用户资料是两套数据，单独用一个 MomentVM
    private val momentVM = MomentVM()
    private var momentAdapter: MomentAdapter? = null

    override fun initShiTu() {
        accountId = arguments?.getInt(ARG_ACCOUNT_ID, 0) ?: 0
        isSelf = accountId == (GlobalConfig.userData?.accountId ?: -1)
        binding.tvFollow.visibility = if (isSelf) View.GONE else View.VISIBLE
        binding.ivMore.visibility = if (isSelf) View.GONE else View.VISIBLE
        binding.tvSendMsg.visibility = if (isSelf) View.GONE else View.VISIBLE

        initMoments()

        mViewModel.loadUserInfo(accountId)
        if (!isSelf && accountId > 0) {
            mViewModel.loadBlockList()
        }
    }

    /**
     * TA 的感悟列表。
     * 列表在 NestedScrollView 里，必须关掉自身滚动，否则滑动手势会被内层吃掉。
     */
    private fun initMoments() {
        if (accountId <= 0) return
        momentAdapter = MomentAdapter(
            requireContext(),
            onItemClick = { entity ->
                openPage(MomentDetailsFragment.newInstance(entity.momentId, entity))
            },
            onVideoClick = { entity ->
                openPage(MomentDetailsFragment.newInstance(entity.momentId, entity))
            },
            onUserClick = { entity -> openUserProfile(entity.postAccountId) }
        )
        binding.rcMoments.layoutManager = LinearLayoutManager(requireContext())
        binding.rcMoments.isNestedScrollingEnabled = false
        binding.rcMoments.adapter = momentAdapter
        momentVM.getMomentListByAccount(accountId, 0)
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

        mViewModel.blockListResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                blocked = result.data?.any { it.targetAccountId == accountId } == true
            }
        }

        mViewModel.reportResult.observe(viewLifecycleOwner) { result ->
            ToastUtil.showMessage(
                getString(if (result.success) R.string.report_done else R.string.report_fail)
            )
        }

        //TA 的感悟列表：空列表时显示占位文案，而不是留一块空白
        momentVM.momentByAccountResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val data = if (result.success) result.data ?: emptyList() else emptyList()
            momentAdapter?.initData(data)
            binding.rcMoments.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
            binding.tvMomentEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
        }

        mViewModel.blockResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                blocked = true
                ToastUtil.showMessage(getString(R.string.block_done))
                //拉黑后关注关系没有意义了，界面上也别再显示"已关注"
                binding.tvFollow.visibility = View.GONE
                //通知各动态列表把 TA 的内容摘掉
                EventBusProxy.post(MessageEvent(EventConstant.EVENT_BLOCK_ACCOUNT, accountId))
            } else {
                ToastUtil.showMessage(getString(R.string.block_fail))
            }
        }

        mViewModel.unblockResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                blocked = false
                ToastUtil.showMessage(getString(R.string.unblock_done))
                binding.tvFollow.visibility = View.VISIBLE
            } else {
                ToastUtil.showMessage(getString(R.string.unblock_fail))
            }
        }
    }

    private fun render(user: PersonData) {
        peerUser = user
        binding.ivAvatar.loadHead(user.avatar)
        binding.tvName.text = user.userName
        com.fingertip.uilib.utils.LevelUi.bindTag(binding.ivLevelTag, user.userLevel ?: 0)
        binding.tvLevel.text = com.fingertip.uilib.utils.LevelUi.tagTitle(user.userLevel ?: 0)
        binding.tvMood.text = user.mood
        binding.tvDisplayId.text = "ID:${user.displayAccountId ?: ""}"
        binding.tvFollowings.text = "${user.followingCount ?: 0}"
        binding.tvFollowers.text = "${user.followerCount ?: 0}"
        binding.tvDonateTotal.text = "${user.coin}"

        //功德捐赠数他人主页服务端置0，隐藏行避免误读
        binding.tvDonateTotal.visibility = if (isSelf) View.VISIBLE else View.GONE
    }

    override fun getClickViews(): List<View> =
        listOf(binding.ivBack, binding.tvFollow, binding.ivMore, binding.tvSendMsg)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvFollow -> {
                if (isSelf) return
                if (followed) mViewModel.unFollow(accountId) else mViewModel.follow(accountId)
            }
            binding.ivMore -> showMoreMenu()
            binding.tvSendMsg -> startChat()
        }
    }

    /** 发起会话：从他人主页直达单聊页 */
    private fun startChat() {
        if (isSelf || accountId <= 0) return
        val user = peerUser ?: return
        if (com.fingertip.uilib.im.ImConfig.isOfficialAccount(accountId)) {
            ToastUtil.showMessage(getString(R.string.im_chat_official_no_reply))
            return
        }
        openPage(
            com.fingertip.uilib.fragment.im.ChatFragment.newInstance(
                com.fingertip.uilib.im.ImConfig.userIdOf(accountId),
                user.userName ?: "",
                user.avatar ?: ""
            )
        )
    }

    /** 各处头像/昵称跳到用户主页 */
    fun openUserProfile(targetAccountId: Int) {
        if (targetAccountId <= 0) return
        openPage(UserInfoFragment.newInstance(targetAccountId))
    }

    /**
     * 更多：举报 / 拉黑 / 取消拉黑
     *
     * 他人主页是「针对人」操作的天然落点——用户从动态详情进来时可能只想看内容，
     * 真想拉黑某个人时多半是先点进主页，所以这一层必须也有一级入口。
     */
    private fun showMoreMenu() {
        if (isSelf || accountId <= 0) return

        val items = mutableListOf(
            ActionSheetDialog.Item(getString(R.string.action_report)) {
                ReportDialog(requireContext()) { tag, content ->
                    //reportType=3 针对用户（1动态 2评论），后端按此归类到用户维度
                    mViewModel.report(accountId, 3, tag, content)
                }.show()
            }
        )

        if (blocked) {
            items.add(ActionSheetDialog.Item(getString(R.string.action_unblock)) {
                mViewModel.cancelBlock(accountId)
            })
        } else {
            items.add(ActionSheetDialog.Item(getString(R.string.action_block), danger = true) {
                ConfirmDialog(
                    requireContext(),
                    title = getString(R.string.block_confirm_title),
                    message = getString(R.string.block_confirm_msg),
                    confirmText = getString(R.string.block_confirm_ok)
                ) {
                    mViewModel.addBlock(accountId)
                }.show()
            })
        }

        ActionSheetDialog(requireContext(), getString(R.string.block_title), items).show()
    }
}

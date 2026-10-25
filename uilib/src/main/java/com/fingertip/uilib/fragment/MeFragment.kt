package com.fingertip.uilib.fragment

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.fingertip.baselib.bean.BannerEntity
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ColorUtil
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MeBannerAdapter
import com.fingertip.uilib.databinding.FragmentMeBinding
import com.fingertip.uilib.fragment.blessing.PrayingMineFragment
import com.fingertip.uilib.fragment.game.FangShengFragment
import com.fingertip.uilib.fragment.user.ProfileEditFragment
import com.fingertip.uilib.viewmodel.UserVM
import com.lzlz.toplib.extention.toPx
import com.youth.banner.Banner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import kotlin.math.min

/**
 * 个人中心主页，对应原型「个人中心」：
 * 用户信息头（头像/昵称/等级/签名/关注/佛友）、功德币/修行值、功能入口列表
 *
 * 已接通：我的祈福/放生记录/我的行善/编辑资料；其余入口（任务中心/兑换中心/我的物品/
 * 功德记录/我的足迹/我的感悟/修行攻略/设置）后端或页面未完成，toast 提示建设中
 */
class MeFragment : TopPmFragment<UserVM>() {
    override fun layoutId(): Int = R.layout.fragment_me
    override fun initVM(): UserVM = provideVM()

    private val binding get() = mBinding as FragmentMeBinding

    override fun initShiTu() {
        initBanner()
        renderUser(GlobalConfig.userData)

        binding.nsl.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val slideOffset = min(scrollY * 1.0f / 88.toPx(), 1f)
            binding.clTitle.setBackgroundColor(ColorUtil.changeAlpha(resources.getColor(R.color.white), slideOffset))
        }

        //拉一次最新用户信息（功德币/修行值可能刚变动）
        GlobalConfig.userData?.accountId?.let { mViewModel.loadUserInfo(it) }
    }

    private fun renderUser(user: PersonData?) {
        user ?: return
        binding.ivAvatar.loadHead(user.avatar)
        binding.ivAvatarTitle.loadHead(user.avatar)
        binding.tvNickname.text = user.userName
        binding.tvNicknameTitle.text = user.userName
        binding.tvLevel.text = "lv${user.userLevel}"
        binding.tvNote.text = user.mood
        binding.tvFollowings.text = "${user.followingCount ?: 0}"
        binding.tvFollowers.text = "${user.followerCount ?: 0}"
        binding.tvCoins.text = "${user.coin}"
        binding.tvExp.text = "${user.userExp ?: 0}"
    }

    var bannerAdapter: MeBannerAdapter? = null
    var bannerList = ArrayList<BannerEntity>()
    private fun initBanner() {
        bannerList.add(BannerEntity().apply { imgUrl = "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b" })
        bannerList.add(BannerEntity().apply { imgUrl = "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b" })
        bannerList.add(BannerEntity().apply { imgUrl = "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b" })
        bannerAdapter = MeBannerAdapter(requireContext(), bannerList) { viewId, pos ->

        }
        val bannerView = binding.banner
        (bannerView as? Banner<BannerEntity, MeBannerAdapter>)?.setAdapter(bannerAdapter)
        bannerView.visibility = if (bannerList.isNullOrEmpty()) View.GONE else View.VISIBLE
        (bannerView as? Banner<BannerEntity, MeBannerAdapter>)?.setIndicator(binding.indicator, false)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.userInfoResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                result.data?.let {
                    GlobalConfig.userData = it
                    renderUser(it)
                }
            } else {
                log("MeFragment", "用户信息获取失败：${result.errorCode} ${result.errorMsg}")
            }
        }
    }

    override fun getClickViews(): List<View> = mutableListOf(
        binding.ivSetting, binding.ivEdit, binding.ivAvatar, binding.tvNickname,
        binding.llTask, binding.llExchange, binding.llItem, binding.llPunna,
        binding.llHistory, binding.llMind, binding.llBless, binding.llReborn, binding.llIntro
    )

    override fun onSingleClick(v: View?) {
        when (v) {
            binding.ivSetting -> ToastUtil.showMessage("设置页建设中")

            binding.ivEdit -> {
                (parentFragment as? MainFragment)?.start(ProfileEditFragment())
            }

            //点头像/昵称看自己的用户信息页
            binding.ivAvatar, binding.tvNickname -> {
                GlobalConfig.userData?.accountId?.let {
                    (parentFragment as? MainFragment)?.start(
                        com.fingertip.uilib.fragment.user.UserInfoFragment.newInstance(it)
                    )
                }
            }

            binding.llTask -> ToastUtil.showMessage("任务中心建设中")
            binding.llExchange -> ToastUtil.showMessage("兑换中心建设中")
            binding.llItem -> ToastUtil.showMessage("我的物品建设中")
            binding.llPunna -> ToastUtil.showMessage("功德记录建设中")
            binding.llHistory -> ToastUtil.showMessage("我的足迹建设中")
            binding.llMind -> ToastUtil.showMessage("我的感悟建设中")
            binding.llBless -> (parentFragment as? MainFragment)?.start(PrayingMineFragment.newInstance(false))
            binding.llReborn -> (parentFragment as? MainFragment)?.start(FangShengFragment())
            binding.llIntro -> ToastUtil.showMessage("修行攻略建设中")
        }
    }

    override fun onVisible() {
        super.onVisible()
        //回到本页时刷新数据
        GlobalConfig.userData?.accountId?.let { mViewModel.loadUserInfo(it) }
    }

    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when (event.what) {

        }
    }
}

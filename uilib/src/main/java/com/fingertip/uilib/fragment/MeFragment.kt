package com.fingertip.uilib.fragment

import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.MyDressData
import com.fingertip.baselib.bean.PersonData
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ColorUtil
import com.fingertip.baselib.util.LevelUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.DressCaseAdapter
import com.fingertip.uilib.databinding.FragmentMeBinding
import com.fingertip.uilib.fragment.blessing.PrayingMineFragment
import com.fingertip.uilib.fragment.game.FangShengFragment
import com.fingertip.uilib.fragment.user.ProfileEditFragment
import com.fingertip.uilib.fragment.user.UserLevelFragment
import com.fingertip.uilib.utils.LevelUi
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.UserVM
import com.lzlz.toplib.extention.isVisible
import com.lzlz.toplib.extention.toPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import kotlin.math.min

/**
 * 个人中心主页，对应原型「个人中心」：
 * 用户信息头（头像/昵称/等级/签名/关注/佛友）、功德币/修行值、功能入口列表
 *
 * 已接通：我的祈福/放生记录/我的行善/编辑资料/修行攻略(H5)/装扮商城/我的装扮/我的收藏/
 * 供灯记录/我的感悟/佛历订阅提醒/设置；「任务中心」后端未完成，布局里直接 gone
 * （App Store 审核对“点了弹建设中”的入口按功能不完整处理，不留活口）
 */
class MeFragment : TopPmFragment<UserVM>() {
    override fun layoutId(): Int = R.layout.fragment_me
    override fun initVM(): UserVM = provideVM()

    private val binding get() = mBinding as FragmentMeBinding

    override fun initShiTu() {
        renderUser(GlobalConfig.userData)

        binding.nsl.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val slideOffset = min(scrollY * 1.0f / 88.toPx(), 1f)
            binding.clTitle.setBackgroundColor(ColorUtil.changeAlpha(resources.getColor(R.color.me_title_base), slideOffset))

            binding.ivAvatarTitle.isVisible = slideOffset >= 1f
            binding.tvNicknameTitle.isVisible = slideOffset >= 1f
        }

        //拉一次最新用户信息（功德币/修行值可能刚变动）
        GlobalConfig.userData?.accountId?.let { mViewModel.loadUserInfo(it) }

        //三格数据（功德币/连续修持/累计诵经）+ 装扮橱窗（§12.6）
        mViewModel.loadPracticeStats()
        mViewModel.loadMeditationTotal()
        mViewModel.loadMyDress()
    }

    private fun renderUser(user: PersonData?) {
        user ?: return
        val level = user.userLevel ?: 0
        binding.ivAvatar.loadHead(user.avatar)
        binding.ivAvatarTitle.loadHead(user.avatar)
        binding.tvNickname.text = user.userName
        binding.tvNicknameTitle.text = user.userName
        //等级徽章图（lv 数字画在图里）+ 头衔文字，全 App 统一口径
        LevelUi.bindTag(binding.ivLevelTag, level)
        binding.tvLevel.text = LevelUi.tagTitle(level)
        binding.tvLevel.setTextColor(LevelUtil.colorOf(level))
        binding.tvNote.text = user.mood
        binding.tvFollowings.text = "${user.followingCount ?: 0}"
        binding.tvFollowers.text = "${user.followerCount ?: 0}"
        binding.tvCoins.text = "${user.coin}"
        binding.tvExp.text = "${user.userExp ?: 0}"
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

        //三格数据：连续修持天数 / 累计诵经次数（服务端为准）
        mViewModel.practiceStatsResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                result.data?.let {
                    binding.tvStreak.text = "${it.currentStreak}"
                    binding.tvSutra.text = "${it.totalCount}"
                }
            }
        }
        //三格数据：累计念佛声数（接口语义就是声数，功德币体系已并入功德币，不再显示功德币）
        mViewModel.meditationTotalResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                binding.tvZen.text = "${result.data ?: 0}"
            }
        }
        //装扮橱窗
        mViewModel.myDressResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                renderDressCase(result.data)
            }
        }
    }

    private var dressCaseAdapter: DressCaseAdapter? = null
    private fun renderDressCase(data: MyDressData?) {
        data ?: return
        val dresses = data.dresses.orEmpty()
        binding.tvDressEmpty.visibility = if (dresses.isEmpty()) View.VISIBLE else View.GONE
        binding.rvDressCase.visibility = if (dresses.isEmpty()) View.GONE else View.VISIBLE
        if (dresses.isEmpty()) return
        if (dressCaseAdapter == null) {
            dressCaseAdapter = DressCaseAdapter(requireContext(), data) {
                //点橱窗项进我的装扮（与菜单入口同页）
                (parentFragment as? MainFragment)?.start(
                    com.fingertip.uilib.fragment.shop.MyDressFragment()
                )
            }
            binding.rvDressCase.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.rvDressCase.adapter = dressCaseAdapter
        }
        dressCaseAdapter?.initData(ArrayList(dresses))
    }

    override fun getClickViews(): List<View> = mutableListOf(
        binding.ivSetting, binding.ivEdit, binding.ivAvatar, binding.tvNickname,
        binding.llExchange, binding.llItem, binding.llCollect, binding.llPunna,
        binding.llHistory, binding.llMind, binding.llBless, binding.llReborn, binding.llIntro,
        binding.llCoins, binding.llExp
    )

    override fun onSingleClick(v: View?) {
        when (v) {
            binding.ivSetting -> (parentFragment as? MainFragment)?.start(
                com.fingertip.uilib.fragment.setting.SettingFragment()
            )

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

            //海外版：「兑换中心」改为装扮商城入口，「我的物品」改为我的装扮
            binding.llCoins -> (parentFragment as? MainFragment)?.start(
                com.fingertip.uilib.fragment.shop.RechargeFragment.newInstance()
            )
            //修行值：进等级详情页（等级体系/经验进度/全部等级表）
            binding.llExp -> openPage(UserLevelFragment.newInstance())
            binding.llExchange -> (parentFragment as? MainFragment)?.start(
                com.fingertip.uilib.fragment.shop.ShopFragment.newInstance()
            )
            binding.llItem -> (parentFragment as? MainFragment)?.start(
                com.fingertip.uilib.fragment.shop.MyDressFragment()
            )
            //我的收藏：佛经 / 感悟 / 寺庙三类内容统一入口
            binding.llCollect -> (parentFragment as? MainFragment)?.start(
                com.fingertip.uilib.fragment.user.CollectListFragment()
            )
            //海外版：功德捐赠功能已下线，这一格改为供灯记录（订单与灯油状态的凭证页）
            binding.llPunna -> (parentFragment as? MainFragment)?.start(
                com.fingertip.uilib.fragment.worshiping.LampRecordFragment()
            )
            //佛历订阅提醒：原「我的足迹」的坑位。提醒是拉日活最实在的一环，优先做实
            binding.llHistory -> (parentFragment as? MainFragment)?.start(
                com.fingertip.uilib.fragment.setting.CalendarRemindFragment()
            )
            //我的感悟：自己发过的动态，长按可删除（UGC 政策要求用户能删自己的内容）
            binding.llMind -> (parentFragment as? MainFragment)?.start(
                com.fingertip.uilib.fragment.moment.MyMomentFragment()
            )
            binding.llBless -> (parentFragment as? MainFragment)?.start(PrayingMineFragment.newInstance(false))
            binding.llReborn -> (parentFragment as? MainFragment)?.start(FangShengFragment())

            //修行攻略：H5 地址由 globalParam.guideUrl 下发，可热更新
            binding.llIntro -> com.fingertip.uilib.activity.CommonWebViewActivity.start(
                requireContext(), "修行攻略", GlobalConfig.globalParam?.guideUrl
            )
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

package com.fingertip.uilib.fragment.user

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.LevelUtil
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentUserLevelBinding
import com.fingertip.uilib.databinding.ItemLevelStageBinding
import com.fingertip.uilib.utils.LevelUi
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.UserVM
import kotlin.math.roundToInt

/**
 * 修行等级详情（个人中心「修行值」点进来）
 *
 * 讲清整个 App 的等级设置：当前等级/头衔、本级经验进度、距下一级还差多少、
 * 十二重境界概览，右下入口进三级页看全部等级表。
 *
 * 所有数字都来自 user_level_config（globalParam 下发），页面不写死等级和经验门槛，
 * 后台调 exp 或改 levelTag，这里自动跟着变。
 */
class UserLevelFragment : TopPmFragment<UserVM>() {

    companion object {
        fun newInstance(): UserLevelFragment = UserLevelFragment()
    }

    override fun layoutId(): Int = R.layout.fragment_user_level

    override fun initVM(): UserVM = provideVM()

    private val binding get() = mBinding as FragmentUserLevelBinding

    override fun getClickViews(): List<View> =
        listOf(binding.ivBack, binding.tvAll, binding.tvAllLevels)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvAll, binding.tvAllLevels -> openPage(UserLevelListFragment.newInstance())
        }
    }

    override fun initShiTu() {
        if (!LevelUtil.hasConfig) {
            //等级表没下发（老版本服务端/配置拉取失败）时，页面仍要能看，只是数字退化为 0
            binding.tvStageDesc.text = getString(R.string.level_config_empty)
        }
        renderHeader()
        buildStages()

        //进页面刷一次用户信息：刚诵完经/刚签到就点进来，这里的经验要跟服务端一致
        GlobalConfig.userData?.accountId?.let { mViewModel.loadUserInfo(it) }
    }

    override fun initObserver() {
        super.initObserver()
        mViewModel.userInfoResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                result.data?.let {
                    GlobalConfig.userData = it
                    renderHeader()
                }
            }
        }
    }

    /** 头部：等级徽章 + 头衔 + 经验进度 */
    private fun renderHeader() {
        val user = GlobalConfig.userData
        val exp = user?.userExp ?: 0
        //服务端已按经验算好等级；userLevel 缺失时用经验反查兜底
        val level = (user?.userLevel ?: 0).let {
            if (it > 0) it else LevelUtil.levelOfExp(exp)
        }
        val tag = LevelUtil.tagOf(level)
        val maxLevel = LevelUtil.maxLevel

        //等级徽章：服务端按级下发的药丸图（lv 数字画在图里）；
        //配置缺失时退化为 tv_badge_lv 的段色文字
        val tagUrl = LevelUtil.tagUrlOf(level)
        if (tagUrl.isNotBlank()) {
            binding.ivBadgeImg.visibility = View.VISIBLE
            binding.ivBadgeImg.loadImg(tagUrl, cropImage = false)
            binding.tvBadgeLv.visibility = View.GONE
        } else {
            binding.ivBadgeImg.visibility = View.GONE
            binding.tvBadgeLv.visibility = View.VISIBLE
            LevelUi.applyTextBadge(binding.tvBadgeLv, level, radiusDp = 8f, horizontalPadDp = 0)
        }

        binding.tvCurTag.text = tag.ifBlank { getString(R.string.level_row_level_only, level) }
        binding.tvCurLevel.text = getString(
            R.string.level_cur_level,
            level,
            LevelUtil.configs.size
        )

        //经验进度：本级区间按 config 的累计门槛算，不是增量
        val end = LevelUtil.expEndOf(level)
        val progress = LevelUtil.progressOf(exp)
        if (end > 0) {
            binding.tvExpValue.text = getString(R.string.level_exp_value, exp, end)
        } else {
            binding.tvExpValue.text = "$exp"
        }
        binding.pbExp.progress = (progress * 100).roundToInt().coerceIn(0, 100)
        binding.tvExpPct.text = getString(R.string.level_exp_pct, (progress * 100).roundToInt())

        if (level >= maxLevel) {
            binding.tvNextTip.text = getString(
                R.string.level_max_tip,
                tag.ifBlank { getString(R.string.level_row_level_only, level) }
            )
        } else {
            val nextTag = LevelUtil.tagOf(level + 1)
                .ifBlank { getString(R.string.level_row_level_only, level + 1) }
            binding.tvNextTip.text = getString(
                R.string.level_next_tip,
                nextTag,
                LevelUtil.expToNextOf(exp)
            )
        }
    }

    /** 十二重境界概览：数据随服务端配置变，不能写死在布局里 */
    private fun buildStages() {
        binding.llStages.removeAllViews()
        val stages = LevelUtil.stagesWithLevels()
        if (stages.isEmpty()) {
            binding.tvStageDesc.setText(R.string.level_config_empty)
            return
        }
        //段数/等级数都按服务端实际配置来，改配置这里不会说谎
        binding.tvStageDesc.text = getString(
            R.string.level_stage_desc_dynamic,
            stages.size,
            LevelUtil.configs.size
        )
        val inflater = LayoutInflater.from(requireContext())
        stages.forEach { (stage, levels) ->
            val item = ItemLevelStageBinding.inflate(inflater, binding.llStages, false)
            //圆点按段上色（bg_level_dot 是纯白 shape，用 tint 着色）
            item.vDot.backgroundTintList = ColorStateList.valueOf(stage.color)
            item.tvStage.text = stage.name
            item.tvRange.text = if (levels.size <= 1) {
                getString(R.string.level_stage_range_single, levels.first().level)
            } else {
                getString(
                    R.string.level_stage_range,
                    levels.first().level,
                    levels.last().level
                )
            }
            item.tvNeed.text =
                getString(R.string.level_stage_need, levels.first().exp)
            //点某一段直接进全表并定位到该段，省得用户在 101 行里自己翻
            item.root.setOnClickListener {
                openPage(UserLevelListFragment.newInstance(levels.first().level))
            }
            binding.llStages.addView(item.root)
        }
    }
}

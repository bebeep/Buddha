package com.fingertip.uilib.fragment.user

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.LevelUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.LevelRow
import com.fingertip.uilib.adapter.LevelRowAdapter
import com.fingertip.uilib.databinding.FragmentUserLevelListBinding
import com.fingertip.uilib.viewmodel.UserVM

/**
 * 全部修行等级表（三级页）
 *
 * 一行一级，把 user_level_config 整张表摊开给用户看：头衔、累计门槛、
 * 相对上一级的增量。当前等级高亮并自动滚到它附近——用户点进来第一个问题
 * 永远是「我现在在哪一级、下一级要多少」。
 */
class UserLevelListFragment : TopPmFragment<UserVM>() {

    companion object {
        private const val ARG_ANCHOR_LEVEL = "anchorLevel"

        /**
         * @param anchorLevel 打开后滚到哪一级；-1 表示滚到当前用户所在等级
         */
        fun newInstance(anchorLevel: Int = -1): UserLevelListFragment =
            UserLevelListFragment().apply {
                arguments = android.os.Bundle().apply {
                    putInt(ARG_ANCHOR_LEVEL, anchorLevel)
                }
            }
    }

    override fun layoutId(): Int = R.layout.fragment_user_level_list

    override fun initVM(): UserVM = provideVM()

    private val binding get() = mBinding as FragmentUserLevelListBinding

    private lateinit var adapter: LevelRowAdapter

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        if (v == binding.ivBack) pop()
    }

    override fun initShiTu() {
        adapter = LevelRowAdapter(requireContext())
        val lm = LinearLayoutManager(requireContext())
        binding.rcLevels.layoutManager = lm
        binding.rcLevels.adapter = adapter
        binding.rcLevels.itemAnimator = null

        val configs = LevelUtil.configs
        if (configs.isEmpty()) {
            adapter.showEmptyNow(getString(R.string.level_config_empty))
            binding.tvSummary.text = getString(R.string.level_config_empty)
            return
        }

        val curLevel = GlobalConfig.userData?.userLevel ?: 0
        val rows = configs.mapIndexed { index, config ->
            LevelRow(
                config = config,
                prevExp = if (index > 0) configs[index - 1].exp else 0,
                isCurrent = config.level == curLevel
            )
        }
        adapter.initData(rows)
        binding.tvSummary.text = getString(
            R.string.level_list_summary,
            configs.size,
            LevelUtil.stagesWithLevels().size
        )

        //定位：从某段点进来就滚到那一段，否则滚到当前等级（留 100dp 余量，
        //让上一级也露出来，看得出"刚升上来"）
        val anchor = arguments?.getInt(ARG_ANCHOR_LEVEL, -1) ?: -1
        val index = if (anchor >= 0) {
            rows.indexOfFirst { it.config.level >= anchor }
        } else {
            rows.indexOfFirst { it.isCurrent }
        }
        if (index >= 0) {
            val offset = (100 * resources.displayMetrics.density).toInt()
            binding.rcLevels.post {
                lm.scrollToPositionWithOffset(index, offset)
            }
        }
    }
}

package com.fingertip.uilib.fragment.wishing

import android.view.View
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.fingertip.baselib.bean.WishRecordBean
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.WishCardAdapter
import com.fingertip.uilib.databinding.FragmentWishTreeBinding
import com.fingertip.uilib.dialog.BlessDialog
import com.fingertip.uilib.dialog.WishEditDialog
import com.fingertip.uilib.dialog.WishRuleDialog
import com.fingertip.uilib.viewmodel.WishVM
import java.util.Calendar

/**
 * 许愿树主页，对应原型「许愿树-主页」与 wishingTree 一组接口
 *
 * - 愿望卡两列瀑布，点击弹祝福弹窗；服务端按「已读」换新一批
 * - 底部开始许愿：今天许过（本地按我的愿望列表第一条判断）则置灰
 */
class WishTreeFragment : TopPmFragment<WishVM>() {

    companion object {
        private const val TAG = "WishTreeFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_wish_tree

    override fun initVM(): WishVM = provideVM()

    private val binding get() = mBinding as FragmentWishTreeBinding

    private lateinit var adapter: WishCardAdapter

    //弹窗们，页面统一持有走 VM
    private var editDialog: WishEditDialog? = null
    private var blessDialog: BlessDialog? = null
    private var ruleDialog: WishRuleDialog? = null

    //是否今天已许愿，进页面时顺便拉我的第一条愿望判断
    private var wishedToday = false

    override fun initShiTu() {
        adapter = WishCardAdapter(requireContext()) { wish ->
            showBlessDialog(wish)
        }
        binding.rcWishTree.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.rcWishTree.adapter = adapter

        mViewModel.loadWishTree()
        checkTodayWished()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.wishTreeResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                val list = result.data.orEmpty()
                adapter.initData(list)
                binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                //把这一批标记已读，服务端下次换一批
                mViewModel.readWishing(list.map { it.id })
            } else {
                log(TAG, "许愿树获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "许愿树加载失败，请稍后再试")
            }
        }

        mViewModel.myWishResult.observe(viewLifecycleOwner) { result ->
            if (!result.success) return@observe
            val first = result.data?.firstOrNull() ?: return@observe
            wishedToday = isToday(first.createDate)
            renderPostButton()
        }

        mViewModel.postWishResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                wishedToday = true
                renderPostButton()
                ToastUtil.showMessage("许愿成功")
                editDialog?.onPostSuccess()
                mViewModel.loadWishTree()
            } else {
                editDialog?.onPostFailed()
                ToastUtil.showMessage(result.errorMsg ?: "许愿失败，请稍后再试")
            }
        }

        mViewModel.blessResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                blessDialog?.onBlessSuccess()
                mViewModel.loadWishTree()
            } else {
                blessDialog?.onBlessFailed(result.errorMsg)
            }
        }

        mViewModel.wishTagsResult.observe(viewLifecycleOwner) { result ->
            if (result.success) editDialog?.renderTags(result.data)
        }
    }

    /**
     * 祝福弹窗。弹之前把服务端推荐标签拉好备用
     */
    private fun showBlessDialog(wish: WishRecordBean) {
        val dialog = BlessDialog(requireContext())
        dialog.render(wish)
        dialog.onSubmit = { wishingId, content ->
            mViewModel.blessWishing(wishingId, content)
        }
        blessDialog = dialog
        dialog.show()
    }

    private fun showEditDialog() {
        val dialog = editDialog ?: WishEditDialog(requireContext()).also {
            it.onSubmit = { content -> mViewModel.postWishing(content) }
            editDialog = it
        }
        mViewModel.loadWishTags()
        dialog.show()
    }

    private fun showRuleDialog() {
        val dialog = ruleDialog ?: WishRuleDialog(requireContext()).also { ruleDialog = it }
        dialog.show()
    }

    /**
     * 顺便拉我的愿望第一条，判断今天是否已许愿（设计稿的倒计时状态）
     */
    private fun checkTodayWished() {
        mViewModel.loadMyWishes(0)
    }

    private fun isToday(createDate: String?): Boolean {
        if (createDate.isNullOrBlank()) return false
        val now = Calendar.getInstance()
        val datePart = createDate.substringBefore(' ')
        val today = "%04d-%02d-%02d".format(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH)
        )
        return datePart == today
    }

    private fun renderPostButton() {
        binding.tvPostWish.text = if (wishedToday) "今日已许愿，明日再来" else "开始许愿"
        binding.tvPostWish.alpha = if (wishedToday) 0.5f else 1f
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack,
        binding.tvRule,
        binding.tvMyWish,
        binding.tvMyBless,
        binding.tvPostWish
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvRule -> showRuleDialog()
            binding.tvMyWish -> (parentFragment as? com.fingertip.uilib.fragment.MainFragment)?.start(MyWishFragment())
            binding.tvMyBless -> (parentFragment as? com.fingertip.uilib.fragment.MainFragment)?.start(MyBlessFragment())
            binding.tvPostWish -> {
                if (wishedToday) return
                showEditDialog()
            }
        }
    }
}

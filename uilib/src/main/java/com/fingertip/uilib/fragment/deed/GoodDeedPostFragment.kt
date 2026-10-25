package com.fingertip.uilib.fragment.deed

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fingertip.baselib.bean.DEED_CATEGORIES
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentDeedPostBinding
import com.fingertip.uilib.viewmodel.GoodDeedVM

/**
 * 发布求助页，对应原型「行善-发布页面」的自主发布
 * 标题限32字、内容必填、分类、目标功德币、加急
 */
class GoodDeedPostFragment : TopPmFragment<GoodDeedVM>() {

    companion object {
        private const val TAG = "GoodDeedPostFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_deed_post

    override fun initVM(): GoodDeedVM = provideVM()

    private val binding get() = mBinding as FragmentDeedPostBinding

    private var category: String? = ""
    private val categoryViews = mutableListOf<Pair<String?, TextView>>()

    override fun initShiTu() {
        renderCategories()
    }

    private fun renderCategories() {
        val container = binding.llCategory
        categoryViews.clear()
        DEED_CATEGORIES.forEach { name ->
            val tv = TextView(requireContext())
            tv.text = name
            tv.textSize = 13f
            tv.setPadding(28, 10, 28, 10)
            tv.gravity = Gravity.CENTER
            tv.setBackgroundResource(R.drawable.bg_praying_cover_normal)
            tv.setTextColor(Color.parseColor("#8A6B3A"))
            tv.setOnClickListener {
                //单选，再点一次取消
                category = if (category == name) "" else name
                renderCategoryState()
            }
            val dp = (resources.displayMetrics.density * 6).toInt()
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp
            lp.bottomMargin = dp
            container.addView(tv, lp)
            categoryViews.add(name to tv)
        }
    }

    private fun renderCategoryState() {
        categoryViews.forEach { (name, tv) ->
            val selected = name == category && category?.isNotEmpty() == true
            tv.setBackgroundResource(
                if (selected) R.drawable.bg_praying_cover_selected else R.drawable.bg_praying_cover_normal
            )
            tv.setTextColor(if (selected) Color.parseColor("#C87A2B") else Color.parseColor("#8A6B3A"))
        }
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.postResult.observe(viewLifecycleOwner) { result ->
            binding.tvSubmit.alpha = 1f
            if (result.success) {
                ToastUtil.showMessage("发布成功，等待佛友伸出援手")
                pop()
            } else {
                log(TAG, "发布求助失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "发布失败，请稍后再试")
            }
        }
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack, binding.tvSubmit)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvSubmit -> {
                val title = binding.etTitle.text?.toString()?.trim().orEmpty()
                val content = binding.etContent.text?.toString()?.trim().orEmpty()
                val target = binding.etTarget.text?.toString()?.toIntOrNull() ?: 0
                if (title.isEmpty()) {
                    ToastUtil.showMessage("标题不能为空")
                    return
                }
                if (content.isEmpty()) {
                    ToastUtil.showMessage("求助内容不能为空")
                    return
                }
                if (target <= 0) {
                    ToastUtil.showMessage("请填写目标功德币")
                    return
                }
                binding.tvSubmit.alpha = 0.6f
                mViewModel.postDeed(
                    title, content, category,
                    if (binding.cbUrgent.isChecked) 1 else 0, target
                )
            }
        }
    }
}

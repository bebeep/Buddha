package com.fingertip.uilib.fragment.wishing

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopApplication
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentWishEditBinding
import com.fingertip.uilib.im.WishBus
import com.fingertip.uilib.viewmodel.WishVM

/**
 * 愿望编辑页，对应原型「许愿-编辑愿望」由弹窗升级为页面：
 * 内容最多 500 字，推荐标签（服务端 getWishingTag）点一下填入。
 * 提交成功 → 拉我的最新愿望 → 放进 WishBus → 返回许愿树播放飞签动画。
 */
class WishEditFragment : TopPmFragment<WishVM>() {

    companion object {
        private const val TAG = "WishEditFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_wish_edit

    override fun initVM(): WishVM = provideVM()

    private val binding get() = mBinding as FragmentWishEditBinding

    //提交中防连点
    private var submitting = false

    override fun initShiTu() {
        binding.etWishContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.tvContentCount.text = "${s?.length ?: 0}/500"
            }
        })
        binding.tvSubmit.setOnClickListener {
            val content = binding.etWishContent.text?.toString()?.trim().orEmpty()
            if (content.isEmpty()) {
                ToastUtil.showMessage("愿望内容不能为空")
                return@setOnClickListener
            }
            if (submitting) return@setOnClickListener
            submitting = true
            binding.tvSubmit.alpha = 0.6f
            val isAnonymous = if (binding.swAnonymous.isChecked) 1 else 0
            mViewModel.postWishing(content, isAnonymous)
        }
        mViewModel.loadWishTags()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.wishTagsResult.observe(viewLifecycleOwner) { result ->
            if (result.success) renderTags(result.data)
        }

        mViewModel.postWishResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                //拉我的愿望第一页，取最新一条作为新愿签
                mViewModel.loadMyWishes(0)
            } else {
                submitting = false
                binding.tvSubmit.alpha = 1f
                log(TAG, "许愿失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "许愿失败，请稍后再试")
            }
        }

        mViewModel.myWishResult.observe(viewLifecycleOwner) { result ->
            if (!result.success) return@observe
            //只在提交流程里消费：拿最新一条放进总线，返回许愿树播放飞签
            val newest = result.data?.firstOrNull()
            if (submitting && newest != null) {
                submitting = false
                WishBus.pendingNewWish = newest
                ToastUtil.showMessage("许愿成功，愿签已挂上许愿树")
                pop()
            }
        }
    }

    /**
     * 填充推荐标签
     */
    private fun renderTags(tags: List<String>?) {
        val container = binding.llTags
        container.removeAllViews()
        tags?.forEach { tag ->
            val tv = TextView(requireContext())
            tv.text = tag
            tv.textSize = 12f
            tv.setTextColor(TopApplication.instance.getColor(com.fingertip.baselib.R.color.c_gold))
            tv.setBackgroundResource(R.drawable.bg_wish_tag)
            tv.setPadding(30, 12, 30, 12)
            tv.gravity = Gravity.CENTER
            tv.setOnClickListener { binding.etWishContent.setText(tag) }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 20
            container.addView(tv, lp)
        }
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
        }
    }
}

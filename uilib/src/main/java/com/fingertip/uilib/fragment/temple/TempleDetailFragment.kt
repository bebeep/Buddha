package com.fingertip.uilib.fragment.temple

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.TempleBean
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.TempleCommentAdapter
import com.fingertip.uilib.databinding.FragmentTempleDetailBinding
import com.fingertip.uilib.viewmodel.TempleVM

/**
 * 寺庙详情，对应原型「寺庙详情」：
 * 封面/称号/推荐(👍👎)/简介/历史/交通/讨论(评论+发送)
 */
class TempleDetailFragment : TopPmFragment<TempleVM>() {

    companion object {
        private const val ARG_TEMPLE_ID = "arg_temple_id"
        private const val TAG = "TempleDetailFragment"

        fun newInstance(templeId: Int): TempleDetailFragment {
            return TempleDetailFragment().apply {
                arguments = Bundle().apply { putInt(ARG_TEMPLE_ID, templeId) }
            }
        }
    }

    override fun layoutId(): Int = R.layout.fragment_temple_detail

    override fun initVM(): TempleVM = provideVM()

    private val binding get() = mBinding as FragmentTempleDetailBinding

    private lateinit var commentAdapter: TempleCommentAdapter
    private var templeId = 0
    private var temple: TempleBean? = null

    override fun initShiTu() {
        templeId = arguments?.getInt(ARG_TEMPLE_ID, 0) ?: 0
        commentAdapter = TempleCommentAdapter(requireContext())
        binding.rcComment.layoutManager = LinearLayoutManager(requireContext())
        binding.rcComment.adapter = commentAdapter

        mViewModel.loadDetail(templeId)
        mViewModel.loadComments(templeId, 0)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.detailResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                render(result.data ?: return@observe)
            } else {
                log(TAG, "寺庙详情获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败")
            }
        }

        mViewModel.commentListResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                commentAdapter.initData(result.data.orEmpty())
                binding.tvCommentHeader.text = "讨论（${temple?.commentCount ?: commentAdapter.itemCount}）"
            }
        }

        mViewModel.recommendResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                ToastUtil.showMessage("感谢反馈")
                mViewModel.loadDetail(templeId)
            } else {
                ToastUtil.showMessage(result.errorMsg ?: "操作失败")
            }
        }

        mViewModel.commentResult.observe(viewLifecycleOwner) { result ->
            binding.tvSend.isEnabled = true
            if (result.success) {
                ToastUtil.showMessage("评论成功")
                binding.etComment.setText("")
                mViewModel.loadComments(templeId, 0)
            } else {
                ToastUtil.showMessage(result.errorMsg ?: "评论失败")
            }
        }
    }

    private fun render(item: TempleBean) {
        temple = item
        binding.tvName.text = item.templeName
        binding.tvTitle.text = item.templeTitle
        binding.tvIntro.text = item.templeIntro
        binding.tvHistory.text = item.templeHistory
        val distance = item.distanceText()
        binding.tvAddr.text = buildString {
            append(item.templeAddr ?: "")
            if (distance.isNotEmpty()) append("（距离约$distance）")
        }

        val cover = item.coverList().firstOrNull()
        if (!cover.isNullOrBlank()) binding.ivCover.loadImg(cover)

        renderRecommendButtons(item)
    }

    private fun renderRecommendButtons(item: TempleBean) {
        when (item.myRecommendStatus) {
            1 -> {
                binding.tvUp.setBackgroundResource(R.drawable.bg_praying_cover_selected)
                binding.tvDown.setBackgroundResource(R.drawable.bg_praying_cover_normal)
            }
            2 -> {
                binding.tvUp.setBackgroundResource(R.drawable.bg_praying_cover_normal)
                binding.tvDown.setBackgroundResource(R.drawable.bg_praying_cover_selected)
            }
            else -> {
                binding.tvUp.setBackgroundResource(R.drawable.bg_praying_cover_normal)
                binding.tvDown.setBackgroundResource(R.drawable.bg_praying_cover_normal)
            }
        }
        binding.tvUp.text = "👍 推荐 ${item.recommendUp}"
        binding.tvDown.text = "👎 不推荐 ${item.recommendDown}"
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack, binding.tvUp, binding.tvDown, binding.tvSend
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvUp -> {
                if (temple?.myRecommendStatus == 1) {
                    ToastUtil.showMessage("已推荐过啦")
                    return
                }
                mViewModel.recommend(templeId, 1)
            }
            binding.tvDown -> {
                if (temple?.myRecommendStatus == 2) {
                    ToastUtil.showMessage("已反馈过啦")
                    return
                }
                mViewModel.recommend(templeId, 2)
            }
            binding.tvSend -> {
                val comment = binding.etComment.text?.toString()?.trim().orEmpty()
                if (comment.isEmpty()) {
                    ToastUtil.showMessage("说点什么吧")
                    return
                }
                binding.tvSend.isEnabled = false
                mViewModel.postComment(templeId, comment)
            }
        }
    }
}

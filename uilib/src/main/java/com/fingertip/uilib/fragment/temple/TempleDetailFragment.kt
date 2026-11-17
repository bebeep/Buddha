package com.fingertip.uilib.fragment.temple

import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.KeyboardUtils
import com.fingertip.baselib.bean.TempleBean
import com.fingertip.baselib.dialog.BigImageDialog
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.R as BaseR
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.TempleCommentAdapter
import com.fingertip.uilib.adapter.TempleCoverAdapter
import com.fingertip.uilib.databinding.FragmentTempleDetailBinding
import com.fingertip.uilib.databinding.ItemTempleHistoryBinding
import com.fingertip.uilib.fragment.user.UserInfoFragment
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.TempleVM
import org.json.JSONArray

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
    private var coverAdapter: TempleCoverAdapter? = null
    private var coverCallback: ViewPager2.OnPageChangeCallback? = null
    //封面轮播当前页（进大图预览时定位用）
    private var coverIndex = 0
    //顶栏是否悬浮在封面之上（true=透明遮罩+白字，false=实底+主题字）
    private var barOverCover = true

    override fun initShiTu() {
        templeId = arguments?.getInt(ARG_TEMPLE_ID, 0) ?: 0
        commentAdapter = TempleCommentAdapter(requireContext(), onUserClick = { comment ->
            if (comment.accountId > 0) {
                openPage(
                    com.fingertip.uilib.fragment.user.UserInfoFragment.newInstance(comment.accountId)
                )
            }
        })
        binding.rcComment.layoutManager = LinearLayoutManager(requireContext())
        binding.rcComment.adapter = commentAdapter

        //软键盘顶起输入栏：容器 Activity 全屏沉浸（isFullTopBar），adjustResize 失效，
        //监听 IME insets 把输入栏垫在键盘上方（取 IME 与导航栏较大者，同 ChatFragment）
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val ime = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            val nav = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = maxOf(ime.bottom, nav.bottom))
            insets
        }

        //键盘弹出时点内容区空白处：收起键盘并取消编辑框焦点
        binding.svContent.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) hideKeyboard()
            false //不消费，滑动手势照常
        }

        //顶栏滚动状态：滚过封面后从"透明遮罩+白字"切到"实底+主题字"
        binding.svContent.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val barH = binding.clTitle.height
            val switchY = (binding.vpCover.height - barH).coerceAtLeast(0)
            val over = scrollY < switchY
            if (over != barOverCover) {
                barOverCover = over
                applyBarStyle()
            }
        }

        mViewModel.loadDetail(templeId)
        mViewModel.loadComments(templeId, 0)
    }

    /** 收起软键盘并取消评论框焦点 */
    private fun hideKeyboard() {
        KeyboardUtils.hideSoftInput(binding.etComment)
        if (binding.etComment.hasFocus()) binding.etComment.clearFocus()
    }

    private fun applyBarStyle() {
        if (barOverCover) {
            binding.clTitle.setBackgroundResource(R.drawable.bg_temple_top_scrim)
            binding.ivBack.setBackgroundResource(R.drawable.bg_back_circle)
            binding.ivBack.setColorFilter(Color.WHITE)
            binding.tvBarTitle.setTextColor(Color.WHITE)
        } else {
            binding.clTitle.setBackgroundResource(BaseR.color.c_bg_card)
            binding.ivBack.setBackgroundResource(0)
            binding.ivBack.clearColorFilter()
            binding.tvBarTitle.setTextColor(ContextCompat.getColor(requireContext(), BaseR.color.c_text_main))
        }
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
                //收起键盘、取消焦点，回到浏览状态
                hideKeyboard()
                mViewModel.loadComments(templeId, 0)
                //评论数显示用的是 detail 里的 temple.commentCount，只刷评论列表数字不动
                mViewModel.loadDetail(templeId)
            } else {
                ToastUtil.showMessage(result.errorMsg ?: "评论失败")
            }
        }
    }

    private fun render(item: TempleBean) {
        temple = item
        binding.tvName.text = item.templeName
        binding.tvBarTitle.text = item.templeName
        binding.tvTitle.text = item.templeTitle
        binding.tvIntro.text = item.templeIntro
        renderHistory(item.templeHistory)
        val distance = item.distanceText()
        binding.tvAddr.text = buildString {
            append(item.templeAddr ?: "")
            if (distance.isNotEmpty()) append("（距离约$distance）")
        }

        setupCoverCarousel(item.coverList())

        renderRecommendButtons(item)
    }

    /**
     * 封面轮播：templeImages 逗号分隔多图，横滑切换 + 页码指示；
     * 点击当前页进全屏大图预览（BigImageDialog，可继续滑动/双指缩放）
     */
    private fun setupCoverCarousel(covers: List<String>) {
        binding.tvCoverIndicator.visibility = View.GONE
        if (covers.isEmpty()) {
            coverAdapter = null
            return
        }

        //detail 刷新（推荐/评论后）会重走这里：列表没变时保留用户当前所在页
        val keepIndex = if (coverAdapter?.itemCount == covers.size) coverIndex else 0

        coverAdapter = TempleCoverAdapter(covers) { pos ->
            BigImageDialog(requireContext(), ArrayList(covers), pos).show()
        }
        coverCallback?.let { binding.vpCover.unregisterOnPageChangeCallback(it) }
        coverCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                coverIndex = position
                binding.tvCoverIndicator.text = "${position + 1}/${covers.size}"
            }
        }.also { binding.vpCover.registerOnPageChangeCallback(it) }
        binding.vpCover.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.vpCover.adapter = coverAdapter
        coverIndex = keepIndex.coerceIn(0, covers.size - 1)
        binding.vpCover.setCurrentItem(coverIndex, false)
        binding.tvCoverIndicator.text = "${coverIndex + 1}/${covers.size}"
        binding.tvCoverIndicator.visibility = if (covers.size > 1) View.VISIBLE else View.GONE
    }

    /**
     * 历史时间线：templeHistory 为 JSON 数组 [{year, content}]，
     * 逐条填充金点+竖线时间线；JSON 解析失败则退回纯文本展示。
     */
    private fun renderHistory(raw: String?) {
        binding.llHistory.removeAllViews()
        if (raw.isNullOrBlank()) return

        val entries = parseHistory(raw)
        if (entries.isEmpty()) {
            //兜底：非法 JSON 时直接显示原文
            binding.llHistory.addView(TextView(requireContext()).apply {
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), BaseR.color.c_text_third))
                setLineSpacing(0f, 1.2f)
                text = raw
            })
            return
        }
        entries.forEach { (year, content) ->
            val item = ItemTempleHistoryBinding.inflate(layoutInflater, binding.llHistory, true)
            item.tvYear.text = year
            item.tvContent.text = content
        }
    }

    private fun parseHistory(raw: String?): List<Pair<String, String>> {
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val y = o.optString("year").trim()
                val c = o.optString("content").trim()
                if (y.isEmpty() && c.isEmpty()) null else y to c
            }
        } catch (e: Exception) {
            log(TAG, "templeHistory JSON 解析失败: ${e.message}")
            emptyList()
        }
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

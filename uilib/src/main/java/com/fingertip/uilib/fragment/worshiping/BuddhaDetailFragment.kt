package com.fingertip.uilib.fragment.worshiping

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.GsonUtils
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.util.loadImg
import com.fingertip.baselib.util.loadImgNoHolder
import com.fingertip.uilib.R
import com.fingertip.uilib.activity.BuddhaVideoPlayActivity
import com.fingertip.uilib.adapter.BuddhaDetailBookAdapter
import com.fingertip.uilib.adapter.BuddhaPhotoAdapter
import com.fingertip.uilib.adapter.BuddhaVideoAdapter
import com.fingertip.uilib.databinding.FragmentBuddhaDetailBinding
import com.fingertip.uilib.dialog.WorshipDialog
import com.fingertip.uilib.fragment.book.BookShelfFragment
import com.fingertip.uilib.fragment.book.FojingDetailsFragment
import com.fingertip.uilib.viewmodel.WorshipVM

/**
 * 佛像详情页，对应原型「佛像详情」：
 * 法相图册 / 诞辰 / 主佑标签 / 推荐佛经 / 简介 / 视频简介 / 底部供奉区（供奉状态+倒计时+供佛按钮）
 *
 * 「选择佛像」页点「供佛」进入本页；在本页选择供奉时长并支付功德币完成供佛
 */
class BuddhaDetailFragment : TopPmFragment<WorshipVM>() {

    companion object {
        private const val TAG = "BuddhaDetailFragment"
        private const val ARG_BUDDHA = "arg_buddha"

        fun newInstance(buddha: BuddhaConfig): BuddhaDetailFragment {
            return BuddhaDetailFragment().apply {
                arguments = Bundle().apply { putSerializable(ARG_BUDDHA, buddha) }
            }
        }
    }

    override fun layoutId(): Int = R.layout.fragment_buddha_detail

    override fun initVM(): WorshipVM = provideVM()

    private val binding get() = mBinding as FragmentBuddhaDetailBinding

    private lateinit var buddha: BuddhaConfig

    /** 供奉剩余秒数，本地倒计时用 */
    private var worshipLeftSec: Long = 0L

    private var countDownTimer: CountDownTimer? = null

    override fun initShiTu() {
        buddha = arguments?.getSerializable(ARG_BUDDHA) as? BuddhaConfig ?: BuddhaConfig()

        binding.tvName.text = buddha.buddhaName ?: "佛像详情"
        binding.ivCover.loadImg(buddha.buddhaCoverUrl)

        initPhotos()
        renderBirthdayAndTags()
        initBooks()

        binding.tvIntro.text = buddha.buddhaIntro?.takeIf { it.isNotBlank() } ?: "暂无简介"

        initVideos()

        initWorshipState()
    }

    override fun initObserver() {
        super.initObserver()
        mViewModel.worshipResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                ToastUtil.showMessage("供佛成功，功德无量")
                //本地更新供奉状态（续供顺延），下次进页面以服务端为准
                worshipDialog?.onWorshipSuccess()
                worshipLeftSec += pendingDays * 24 * 3600L
                startCountDown()
                refreshWorshipUi()
            } else {
                worshipDialog?.onWorshipFailed()
            }
        }
    }

    //region 法相图册
    private fun initPhotos() {
        val photos = parsePictures(buddha.buddhaPicture)
        binding.rcPhotos.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rcPhotos.adapter = BuddhaPhotoAdapter(requireContext(), photos)
    }

    /**
     * 解析图册：兼容 JSON 数组 / 逗号分隔 / 单个 URL
     */
    private fun parsePictures(raw: String?): List<String> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return emptyList()
        return if (text.startsWith("[")) {
            try {
                GsonUtils.fromJson(text, GsonUtils.getListType(String::class.java)) ?: emptyList()
            } catch (e: Exception) {
                log(TAG, "解析图册失败: ${e.message}")
                emptyList()
            }
        } else {
            text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
    //endregion

    //region 诞辰与主佑标签
    private fun renderBirthdayAndTags() {
        binding.tvBirthday.text = buddha.buddhaBirthday?.takeIf { it.isNotBlank() } ?: "—"

        val container = binding.llTags
        container.removeAllViews()
        buddha.buddhaTag?.filterNotNull()?.filter { it.isNotBlank() }?.forEach { tag ->
            val tv = TextView(context)
            tv.text = tag
            tv.textSize = 12f
            tv.setTextColor(Color.parseColor("#C89A5B"))
            tv.setBackgroundResource(R.drawable.bg_buddha_tag)
            tv.setPadding(28, 8, 28, 8)
            tv.gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 20
            container.addView(tv, lp)
        }
    }
    //endregion

    //region 推荐佛经
    private fun initBooks() {
        val books = buddha.recommendBooks?.filterNotNull().orEmpty()
        binding.rcBooks.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rcBooks.adapter = BuddhaDetailBookAdapter(requireContext(), books) { book ->
            if (book.bookId > 0) {
                start(FojingDetailsFragment.newInstance(book.bookId))
            }
        }
        binding.tvBookMore.setOnClickListener {
            start(BookShelfFragment())
        }
    }
    //endregion

    //region 视频简介
    private fun initVideos() {
        val videos = buddha.buddhaVideos?.filterNotNull().orEmpty()
        if (videos.isEmpty()) {
            binding.tvVideoTitle.visibility = View.GONE
            binding.vpVideos.visibility = View.GONE
            return
        }
        val adapter = BuddhaVideoAdapter { position ->
            BuddhaVideoPlayActivity.start(requireContext(), videos, position)
        }
        adapter.submit(videos)
        binding.vpVideos.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.vpVideos.offscreenPageLimit = 1
        binding.vpVideos.adapter = adapter

        //设计稿：一屏一卡居中，左右各露出相邻卡片的边缘。
        //ViewPager2 自身不支持 padding，往内部 RecyclerView 上加，配合 clipToPadding=false 实现
        binding.vpVideos.post {
            (binding.vpVideos.getChildAt(0) as? RecyclerView)?.apply {
                val cardWidth = (resources.displayMetrics.widthPixels * 0.685f).toInt()
                val sidePadding = ((resources.displayMetrics.widthPixels - cardWidth) / 2f).toInt()
                setPadding(sidePadding, 0, sidePadding, 0)
                clipToPadding = false
                clipChildren = false
            }
        }
    }
    //endregion

    //region 供奉状态与供佛
    private var worshipDialog: WorshipDialog? = null
    private var pendingDays = 0

    private fun initWorshipState() {
        worshipLeftSec = buddha.worshipLeftTime
        refreshWorshipUi()
        if (buddha.isWorshipping) startCountDown()

        binding.tvWorship.setOnClickListener {
            val dialog = WorshipDialog(requireContext())
            worshipDialog = dialog
            dialog.render(buddha)
            dialog.onSubmit = { days, _ ->
                pendingDays = days
                mViewModel.worshipBuddha(buddha.id, days)
            }
            dialog.show()
        }
    }

    private fun refreshWorshipUi() {
        val worshipping = worshipLeftSec > 0
        binding.tvWorshipState.text = if (worshipping) "供奉中 · 功德日增" else "尚未供奉"
        binding.tvWorshipLeft.visibility = if (worshipping) View.VISIBLE else View.GONE
    }

    private fun startCountDown() {
        countDownTimer?.cancel()
        if (worshipLeftSec <= 0) return
        countDownTimer = object : CountDownTimer(worshipLeftSec * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                worshipLeftSec = millisUntilFinished / 1000
                binding.tvWorshipLeft.text = "剩余 ${formatLeft(worshipLeftSec)}"
            }

            override fun onFinish() {
                worshipLeftSec = 0
                refreshWorshipUi()
            }
        }.start()
    }

    @SuppressLint("DefaultLocale")
    private fun formatLeft(sec: Long): String {
        val d = sec / 86400
        val h = sec % 86400 / 3600
        val m = sec % 3600 / 60
        val s = sec % 60
        return if (d > 0) "${d}天${String.format("%02d:%02d:%02d", h, m, s)}"
        else String.format("%02d:%02d:%02d", h, m, s)
    }
    //endregion

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        countDownTimer = null
    }
}

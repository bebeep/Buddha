package com.fingertip.uilib.activity

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.event_bus.EventBusProxy
import com.fingertip.baselib.event_bus.EventConstant
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopActivity
import com.fingertip.baselib.util.FullTopUtil
import com.fingertip.uilib.adapter.MomentVideoFeedAdapter
import com.fingertip.uilib.databinding.ActivityMomentVideoFeedBinding
import com.fingertip.uilib.dialog.MomentCommentDialog
import com.fingertip.uilib.viewmodel.MomentVM

/**
 * 佛友圈短视频全屏浏览。
 *
 * 从列表点某条视频进来，只把「有视频的动态」放进 ViewPager2（竖向），
 * 上下滑切换，行为和抖音一致；返回后通过 EventBus 把点赞等改动同步回列表。
 */
class MomentVideoFeedActivity : TopActivity() {

    companion object {
        /**
         * 列表页手里的 MomentEntity 不一定能安全地塞进 Intent（每条带图片/长文），
         * 同进程直接交接引用，Intent 只负责把 Activity 拉起来
         */
        private var pendingVideos: ArrayList<MomentEntity> = ArrayList()
        private var pendingIndex = 0

        fun start(context: Context, videos: List<MomentEntity>, index: Int) {
            if (videos.isEmpty()) return
            pendingVideos = ArrayList(videos)
            pendingIndex = index.coerceIn(0, videos.size - 1)
            context.startActivity(Intent(context, MomentVideoFeedActivity::class.java))
        }
    }

    private lateinit var binding: ActivityMomentVideoFeedBinding
    private lateinit var adapter: MomentVideoFeedAdapter

    private var commentDialog: MomentCommentDialog? = null

    private val momentVM by lazy { ViewModelProvider(this)[MomentVM::class.java] }

    override fun isFullTopBar() = true

    override fun initShiTu() {
        binding = ActivityMomentVideoFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //黑底，状态栏图标要白的
        FullTopUtil.setStatusBarDarkTheme(this, false)

        adapter = MomentVideoFeedAdapter(
            onLikeClick = { entity -> toggleLike(entity) },
            onCommentClick = { entity -> showComments(entity) }
        )
        adapter.submit(pendingVideos)
        pendingVideos = ArrayList()

        binding.pager.orientation = ViewPager2.ORIENTATION_VERTICAL
        //预建前后各一页，滑过去直接有画面，不用等 bind
        binding.pager.offscreenPageLimit = 1
        binding.pager.adapter = adapter
        binding.pager.setCurrentItem(pendingIndex, false)
        adapter.switchTo(pendingIndex)
        updateCounter(pendingIndex)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                adapter.switchTo(position)
                updateCounter(position)
                adapter.entityAt(position)?.let { momentVM.viewMoment(listOf(it.momentId)) }
            }
        })

        binding.ivBack.setOnClickListener { finish() }
    }

    private fun updateCounter(position: Int) {
        val total = adapter.count()
        binding.tvCounter.visibility = if (total > 1) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvCounter.text = "${position + 1}/$total"
    }

    private fun toggleLike(entity: MomentEntity) {
        momentVM.likeMoment(entity.momentId, !entity.isLiked) { like, success ->
            if (!success) return@likeMoment
            entity.isLiked = like
            entity.likeCount = (entity.likeCount + if (like) 1 else -1).coerceAtLeast(0)
            adapter.updateLike(entity.momentId, entity.isLiked, entity.likeCount)
            //列表页也拿的是同一个对象，通知它刷新一下点击态
            EventBusProxy.post(MessageEvent(EventConstant.EVENT_NOTIFY_MOMENT_LIST, entity))
        }
    }

    /**
     * 评论面板：评论数变了只刷新当前页的计数，同时通知列表页
     */
    private fun showComments(entity: MomentEntity) {
        if (commentDialog?.isShowing == true) return
        val dialog = MomentCommentDialog(this, entity.momentId, entity.commentCount)
        dialog.onCommentCountChanged = { count ->
            entity.commentCount = count
            adapter.updateCommentCount(entity.momentId, count)
            EventBusProxy.post(MessageEvent(EventConstant.EVENT_NOTIFY_MOMENT_LIST, entity))
        }
        commentDialog = dialog
        dialog.show()
    }

    override fun onPause() {
        super.onPause()
        if (::adapter.isInitialized) adapter.pauseCurrent()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) adapter.resumeCurrent()
    }

    override fun onDestroy() {
        if (commentDialog?.isShowing == true) commentDialog?.dismiss()
        commentDialog = null
        if (::adapter.isInitialized) adapter.release()
        super.onDestroy()
    }
}

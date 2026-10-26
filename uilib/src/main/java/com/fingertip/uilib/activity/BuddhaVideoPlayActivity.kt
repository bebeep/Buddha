package com.fingertip.uilib.activity

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.fingertip.baselib.bean.BuddhaVideo
import com.fingertip.baselib.top.TopActivity
import com.fingertip.baselib.util.FullTopUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ActivityBuddhaVideoPlayBinding
import com.fingertip.uilib.databinding.ItemBuddhaVideoPageBinding
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView

/**
 * 佛像介绍视频全屏播放页。
 *
 * 从佛像详情「视频简介」点卡片进来，竖向 ViewPager2 上下滑切换视频，
 * 行为参考 MomentVideoFeedActivity，但不带点赞评论，只保留播放与标题。
 */
class BuddhaVideoPlayActivity : TopActivity() {

    companion object {
        /** 列表对象直接同进程交接引用，Intent 只负责拉起 Activity */
        private var pendingVideos: ArrayList<BuddhaVideo> = ArrayList()
        private var pendingIndex = 0

        fun start(context: Context, videos: List<BuddhaVideo>, index: Int) {
            if (videos.isEmpty()) return
            pendingVideos = ArrayList(videos.filterNotNull())
            pendingIndex = index.coerceIn(0, pendingVideos.size - 1)
            context.startActivity(Intent(context, BuddhaVideoPlayActivity::class.java))
        }
    }

    private lateinit var binding: ActivityBuddhaVideoPlayBinding
    private lateinit var adapter: PageAdapter

    override fun isFullTopBar() = true

    override fun initShiTu() {
        binding = ActivityBuddhaVideoPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //黑底，状态栏图标要白的
        FullTopUtil.setStatusBarDarkTheme(this, false)

        adapter = PageAdapter(pendingVideos)
        pendingVideos = ArrayList()

        binding.pager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.pager.offscreenPageLimit = 1
        binding.pager.adapter = adapter
        binding.pager.setCurrentItem(pendingIndex, false)
        adapter.switchTo(pendingIndex)
        renderPageInfo(pendingIndex)

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                adapter.switchTo(position)
                renderPageInfo(position)
            }
        })

        binding.ivBack.setOnClickListener { finish() }
    }

    private fun renderPageInfo(position: Int) {
        val video = adapter.videoAt(position) ?: return
        val total = adapter.itemCount
        binding.tvCounter.text = "${position + 1}/$total"
        binding.tvTitle.text = video.videoTitle.orEmpty()
        binding.tvTitle.visibility =
            if (video.videoTitle.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
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
        if (::adapter.isInitialized) adapter.release()
        super.onDestroy()
    }

    /**
     * 全局只有一个 GSY 播放核心：换页先停旧页，再让新页 setUp + start
     */
    private class PageAdapter(private val items: List<BuddhaVideo>) :
        RecyclerView.Adapter<PageAdapter.PageHolder>() {

        private var recyclerView: RecyclerView? = null
        private var playingHolder: PageHolder? = null
        private var currentPosition = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val binding = ItemBuddhaVideoPageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return PageHolder(binding)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            holder.bind(items[position])
            if (position == currentPosition) {
                //等一帧让页面量完，避免 surface 未就绪
                holder.itemView.post {
                    if (holder.bindingAdapterPosition == currentPosition) startIfNeeded(holder)
                }
            }
        }

        override fun onViewRecycled(holder: PageHolder) {
            super.onViewRecycled(holder)
            if (holder === playingHolder) {
                holder.pause()
                playingHolder = null
            }
            holder.releaseIfIdle()
        }

        override fun onAttachedToRecyclerView(rv: RecyclerView) {
            super.onAttachedToRecyclerView(rv)
            recyclerView = rv
        }

        override fun onDetachedFromRecyclerView(rv: RecyclerView) {
            super.onDetachedFromRecyclerView(rv)
            recyclerView = null
            playingHolder = null
        }

        fun videoAt(position: Int): BuddhaVideo? = items.getOrNull(position)

        fun switchTo(position: Int) {
            if (position !in items.indices) return
            currentPosition = position
            val holder = recyclerView?.findViewHolderForAdapterPosition(position) as? PageHolder
            if (holder != null) {
                startIfNeeded(holder)
            } else {
                //页面还没绑定出来，onBindViewHolder 里会补上
                recyclerView?.post {
                    (recyclerView?.findViewHolderForAdapterPosition(currentPosition) as? PageHolder)
                        ?.let { startIfNeeded(it) }
                }
            }
        }

        fun startIfNeeded(holder: PageHolder) {
            if (playingHolder !== holder) {
                playingHolder?.pause()
                playingHolder = holder
            }
            holder.play()
        }

        fun pauseCurrent() = playingHolder?.pause()

        fun resumeCurrent() = playingHolder?.resume()

        fun release() {
            playingHolder = null
            GSYVideoManager.releaseAllVideos()
        }

        inner class PageHolder(val binding: ItemBuddhaVideoPageBinding) :
            RecyclerView.ViewHolder(binding.root) {

            private var video: BuddhaVideo? = null
            private var playedUrl: String? = null

            init {
                binding.vTap.setOnClickListener { togglePlay() }
            }

            fun bind(item: BuddhaVideo) {
                this.video = item
                binding.video.loadCoverImage(
                    item.videoCover, com.fingertip.baselib.R.mipmap.icon_default_img
                )
                //holder 复用时上一次的播放状态作废
                playedUrl = null
            }

            fun play() {
                val url = video?.videoUrl
                if (url.isNullOrEmpty()) return
                val player = binding.video
                val state = player.currentState
                if (playedUrl == url) {
                    when (state) {
                        GSYVideoView.CURRENT_STATE_PLAYING,
                        GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START -> return
                        GSYVideoView.CURRENT_STATE_PAUSE -> {
                            player.onVideoResume()
                            return
                        }
                        GSYVideoView.CURRENT_STATE_PREPAREING -> return
                    }
                }
                playedUrl = url
                player.setUp(url, true, null)
                player.setIsTouchWiget(false)
                player.isLooping = true
                player.startPlayLogic()
            }

            fun pause() {
                val player = binding.video
                if (player.currentState == GSYVideoView.CURRENT_STATE_PLAYING ||
                    player.currentState == GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START
                ) {
                    player.onVideoPause()
                }
            }

            fun resume() {
                if (binding.video.currentState == GSYVideoView.CURRENT_STATE_PAUSE) {
                    binding.video.onVideoResume()
                }
            }

            fun togglePlay() {
                val player = binding.video
                when (player.currentState) {
                    GSYVideoView.CURRENT_STATE_PLAYING,
                    GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START -> player.onVideoPause()

                    GSYVideoView.CURRENT_STATE_PAUSE -> player.onVideoResume()

                    else -> play()
                }
            }

            fun releaseIfIdle() {
                val player = binding.video
                val state = player.currentState
                if (state == GSYVideoView.CURRENT_STATE_NORMAL ||
                    state == GSYVideoView.CURRENT_STATE_AUTO_COMPLETE ||
                    state == GSYVideoView.CURRENT_STATE_ERROR
                ) {
                    player.release()
                    playedUrl = null
                }
            }
        }
    }
}

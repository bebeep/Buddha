package com.fingertip.uilib.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.util.TimeUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.databinding.ItemMomentVideoPageBinding
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView

/**
 * 短视频全屏浏览的适配器。
 *
 * 全局只有一个 GSY 播放核心，所以这里维护「当前在播的那一页」：
 * 换页时先暂停旧的，再让新的一页 setUp + start，避免两个 surface 抢同一个播放器。
 */
class MomentVideoFeedAdapter(
    private val onLikeClick: (entity: MomentEntity) -> Unit,
    private val onCommentClick: (entity: MomentEntity) -> Unit
) : RecyclerView.Adapter<MomentVideoFeedAdapter.PageHolder>() {

    companion object {
        /** 居中裁剪的最大放大倍数，横屏视频不至于被裁得只剩中间一条 */
        private const val MAX_CROP_SCALE = 1.35f
    }

    private val items = ArrayList<MomentEntity>()
    private var recyclerView: RecyclerView? = null

    /** 当前在播的页，换页/回收/退出时要用它停播 */
    private var playingHolder: PageHolder? = null

    var currentPosition = 0
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val binding = ItemMomentVideoPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        holder.bind(items[position])
        if (position == currentPosition) {
            //ViewPager2 第一次创建页面时还没量完，等一帧再起播，避免 surface 没就绪白跑一次 prepare
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
        holder.releaseSurfaceIfIdle()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
        playingHolder = null
    }

    fun submit(list: List<MomentEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun count(): Int = items.size

    fun entityAt(position: Int): MomentEntity? = items.getOrNull(position)

    /**
     * 切到第 position 页：暂停上一页，起播这一页
     */
    fun switchTo(position: Int) {
        if (position !in items.indices) return
        if (position == currentPosition && playingHolder != null) return
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

    fun pauseCurrent() {
        playingHolder?.pause()
    }

    fun resumeCurrent() {
        playingHolder?.resume()
    }

    fun release() {
        playingHolder = null
        GSYVideoManager.releaseAllVideos()
    }

    /**
     * 点赞回来后只刷新那一页的点赞状态，不要 notifyItemChanged —— 会重建页面、把视频打断
     */
    fun updateLike(momentId: Int, isLiked: Boolean, likeCount: Int) {
        val index = items.indexOfFirst { it.momentId == momentId }
        if (index < 0) return
        val item = items[index]
        item.isLiked = isLiked
        item.likeCount = likeCount
        val holder = recyclerView?.findViewHolderForAdapterPosition(index) as? PageHolder ?: return
        holder.renderLike(item)
    }

    /**
     * 评论数变了同样只改这一页的文本
     */
    fun updateCommentCount(momentId: Int, commentCount: Int) {
        val index = items.indexOfFirst { it.momentId == momentId }
        if (index < 0) return
        val item = items[index]
        item.commentCount = commentCount
        val holder = recyclerView?.findViewHolderForAdapterPosition(index) as? PageHolder ?: return
        holder.renderCommentCount(item)
    }

    private fun startIfNeeded(holder: PageHolder) {
        if (playingHolder !== holder) {
            playingHolder?.pause()
            playingHolder = holder
        }
        holder.play()
    }

    inner class PageHolder(val binding: ItemMomentVideoPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var entity: MomentEntity? = null

        /** 这个 view 上已经 setUp 过的地址，用来判断是续播还是重新 prepare */
        private var playedUrl: String? = null

        init {
            //播放器会按视频比例测量自己（可能比屏幕矮），量完后按「铺满」放大，
            //超出的部分由外层 clipChildren 裁掉，效果就是抖音那种居中裁剪
            binding.video.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                applyCoverScale()
            }
        }

        fun bind(item: MomentEntity) {
            entity = item
            binding.ivHead.loadHead(item.postAvatar)
            binding.tvName.text = item.postUserName.orEmpty()

            val content = item.textContent.orEmpty()
            binding.tvContent.text = content
            binding.tvContent.visibility = if (content.isEmpty()) View.GONE else View.VISIBLE

            val time = TimeUtil.dateFormateTime(item.createDate)
            val locate = item.postAddress.orEmpty()
            binding.tvTime.text = if (locate.isEmpty()) time else "$locate · $time"
            binding.tvTime.visibility = if (time.isEmpty() && locate.isEmpty()) View.GONE else View.VISIBLE

            renderLike(item)
            renderCommentCount(item)

            binding.video.loadCoverImage(item.videoCover, com.fingertip.baselib.R.mipmap.icon_default_img)
            //holder 被复用成另一条动态时，之前那次的播放状态作废
            playedUrl = null

            binding.ivLike.setOnClickListener { entity?.let(onLikeClick) }
            binding.ivComment.setOnClickListener { entity?.let(onCommentClick) }
            binding.vTap.setOnClickListener { togglePlay() }
        }

        fun renderLike(item: MomentEntity) {
            binding.ivLike.isSelected = item.isLiked
            binding.tvLike.text = "${item.likeCount}"
        }

        fun renderCommentCount(item: MomentEntity) {
            binding.tvComment.text = "${item.commentCount}"
        }

        fun play() {
            val url = entity?.videoUrl
            if (url.isNullOrEmpty()) return
            val video = binding.video
            val state = video.currentState
            if (playedUrl == url) {
                when (state) {
                    GSYVideoView.CURRENT_STATE_PLAYING,
                    GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START -> return
                    GSYVideoView.CURRENT_STATE_PAUSE -> {
                        video.onVideoResume()
                        return
                    }
                    GSYVideoView.CURRENT_STATE_PREPAREING -> return
                }
            }
            playedUrl = url
            video.setUp(url, true, null)
            video.setIsTouchWiget(false)
            video.isLooping = true
            video.startPlayLogic()
        }

        fun pause() {
            val video = binding.video
            if (video.currentState == GSYVideoView.CURRENT_STATE_PLAYING ||
                video.currentState == GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START
            ) {
                video.onVideoPause()
            }
        }

        fun resume() {
            val video = binding.video
            if (video.currentState == GSYVideoView.CURRENT_STATE_PAUSE) video.onVideoResume()
        }

        fun togglePlay() {
            val video = binding.video
            when (video.currentState) {
                GSYVideoView.CURRENT_STATE_PLAYING,
                GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START -> video.onVideoPause()

                GSYVideoView.CURRENT_STATE_PAUSE -> video.onVideoResume()

                else -> play()
            }
        }

        /**
         * 被回收时如果没在播，顺手把 surface 释放掉，别让列表里堆一堆 TextureView
         */
        fun releaseSurfaceIfIdle() {
            val video = binding.video
            val state = video.currentState
            if (state == GSYVideoView.CURRENT_STATE_NORMAL ||
                state == GSYVideoView.CURRENT_STATE_AUTO_COMPLETE ||
                state == GSYVideoView.CURRENT_STATE_ERROR
            ) {
                video.release()
                playedUrl = null
            }
        }

        /**
         * 视频按比例测量出来通常比屏幕小（上下有黑边），按 max(宽比, 高比) 放大到铺满，
         * 再靠父容器的 clipChildren 裁边。横屏视频放大会裁掉太多内容，限制最大 1.35 倍。
         */
        fun applyCoverScale() {
            val video = binding.video
            val parent = video.parent as? View ?: return
            val videoWidth = video.width.toFloat()
            val videoHeight = video.height.toFloat()
            if (videoWidth <= 0f || videoHeight <= 0f) return
            if (parent.width <= 0 || parent.height <= 0) return

            val scale = maxOf(parent.width / videoWidth, parent.height / videoHeight)
                .coerceAtMost(MAX_CROP_SCALE)
            if (kotlin.math.abs(video.scaleX - scale) > 0.01f) {
                video.scaleX = scale
                video.scaleY = scale
            }
        }
    }
}

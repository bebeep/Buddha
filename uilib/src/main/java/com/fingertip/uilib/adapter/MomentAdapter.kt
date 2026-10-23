package com.fingertip.uilib.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.dialog.BigImageDialog
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.TimeUtil
import com.fingertip.baselib.util.loadHead
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemMomentBinding

/**
 * 佛友圈列表。
 *
 * 性能相关的几点（滑动卡顿主要来自这里）：
 * 1. ViewHolder 自己 inflate 出 ItemMomentBinding，不走基类的反射找 Binding；
 * 2. 图片九宫格的 adapter/LayoutManager 跟着 holder 复用，不再每次 bind 都 new 一套；
 * 3. 视频容器只在尺寸真的变了才改 LayoutParams，避免每帧 requestLayout；
 * 4. 封面只在地址变了才重新 load。
 */
class MomentAdapter(
    context: Context,
    val onItemClick: (entity: MomentEntity) -> Unit,
    val onVideoClick: (entity: MomentEntity) -> Unit = {}
) : TopRcAdapter<MomentEntity, MomentAdapter.MomentVH>(context) {

    /** 视频容器最大宽度（屏宽 - 左右各 16dp 边距，和布局里的 margin 对齐） */
    private val videoMaxWidth: Int by lazy {
        val dm = context.resources.displayMetrics
        dm.widthPixels - (dm.density * 16 * 2).toInt()
    }

    var bigImageDialog: BigImageDialog? = null

    override fun initLayoutId(viewType: Int): Int = R.layout.item_moment

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MomentVH {
        val binding = ItemMomentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MomentVH(binding)
    }

    override fun onBindViewHolder(holder: MomentVH, position: Int) {
        val binding = holder.binding
        val entity = get(position) ?: return

        binding.vDivider.visibility = if (position == mlist.size - 1) View.GONE else View.VISIBLE
        binding.flImageVideo.visibility = if (entity.momentType == 1) View.GONE else View.VISIBLE
        binding.cvVideo.visibility = if (entity.momentType == 2) View.VISIBLE else View.GONE
        binding.rvPhotos.visibility = if (entity.momentType == 3) View.VISIBLE else View.GONE
        binding.tvContent.visibility = if (entity.textContent.isNullOrEmpty()) View.GONE else View.VISIBLE

        binding.tvContent.text = entity.textContent
        binding.tvName.text = entity.postUserName
        binding.tvLevel.text = "lv.${entity.userLevel}"
        binding.ivHead.loadHead(entity.postAvatar)
        binding.tvLike.text = "${entity.likeCount}"
        binding.tvComment.text = "${entity.commentCount}"
        binding.tvFollow.isSelected = entity.isFollowed
        binding.tvLike.isSelected = entity.isLiked
        binding.tvTimeLocate.text = "${TimeUtil.dateFormateTime(entity.createDate)}·${entity.postAddress}"

        when (entity.momentType) {
            2 -> bindVideo(holder, entity)
            3 -> bindPhotos(holder, entity)
        }

        holder.itemView.setOnClickListener { onItemClick(entity) }
    }

    private fun bindVideo(holder: MomentVH, entity: MomentEntity) {
        val binding = holder.binding
        //根据视频宽高比设置容器高度；只在尺寸变化时赋值，避免整条 item 重新测量
        //服务端偶尔不给宽高，兜底按 16:9，别让容器留成 0 高
        val videoWidth = entity.videoWidth.takeIf { it > 0 } ?: 16
        val videoHeight = entity.videoHeight.takeIf { it > 0 } ?: 9
        val targetWidth: Int
        val targetHeight: Int
        if (videoHeight > videoWidth) {
            targetWidth = (videoMaxWidth * 0.6f).toInt()
            targetHeight = (targetWidth * videoHeight.toFloat() / videoWidth).toInt()
        } else {
            targetWidth = videoMaxWidth
            targetHeight = (targetWidth * videoHeight.toFloat() / videoWidth).toInt()
        }
        val lp = binding.cvVideo.layoutParams
        if (lp.width != targetWidth || lp.height != targetHeight) {
            lp.width = targetWidth
            lp.height = targetHeight
            binding.cvVideo.layoutParams = lp
        }

        //封面只在换了视频时重新加载，来回滑动不重复 decode
        if (holder.coverUrl != entity.videoCover) {
            holder.coverUrl = entity.videoCover
            binding.video.loadCoverImage(entity.videoCover, com.fingertip.baselib.R.mipmap.icon_default_img)
        }
        binding.vVideoTap.setOnClickListener { onVideoClick(entity) }
    }

    private fun bindPhotos(holder: MomentVH, entity: MomentEntity) {
        val binding = holder.binding
        val photos = entity.imageUrl
        holder.currentPhotos = photos
        val columns = when (photos.size) {
            1 -> 1 //单图
            4 -> 2 //两列
            else -> 3 //三列
        }
        if (holder.photoAdapter == null) {
            val grid = GridLayoutManager(context, columns)
            val imageAdapter = MomentImageAdapter(context) { pos ->
                //holder 会被复用，点开的是哪一组图要现取，不能在创建时捕死
                bigImageDialog = BigImageDialog(context, holder.currentPhotos, pos)
                bigImageDialog?.show()
            }
            binding.rvPhotos.layoutManager = grid
            binding.rvPhotos.adapter = imageAdapter
            holder.photoGrid = grid
            holder.photoAdapter = imageAdapter
        } else if (holder.photoGrid?.spanCount != columns) {
            holder.photoGrid?.spanCount = columns
        }
        holder.photoAdapter?.initData(photos)
    }

    class MomentVH(val binding: ItemMomentBinding) : TopRcViewHolder(binding.root) {
        /** 九宫格相关，跟 holder 一起复用 */
        var photoGrid: GridLayoutManager? = null
        var photoAdapter: MomentImageAdapter? = null

        /** 当前封面地址，避免重复 load */
        var coverUrl: String? = null

        /** 当前绑定的图片集合，点击大图时现取 */
        var currentPhotos: List<String?> = emptyList()
    }
}

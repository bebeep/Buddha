package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.fingertip.baselib.R as BaseR
import com.fingertip.baselib.bean.WishRecordBean
import com.fingertip.baselib.util.loadHead
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.DialogWishDetailBinding

/**
 * 愿望详情底部弹窗：点愿签牌弹出，查看他人公开愿望。
 * 可点赞（toggle，乐观更新）、可祝福（内容必填，走页面 VM 统一请求）。
 */
class WishDetailDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    //点「祝愿」，请求交给外部（页面统一走 VM）
    var onSubmitBless: ((wishingId: Int, content: String) -> Unit)? = null

    //点点赞，请求交给外部（页面统一走 VM）
    var onToggleLike: ((wishingId: Int) -> Unit)? = null

    private lateinit var binding: DialogWishDetailBinding
    private var wishRecord: WishRecordBean? = null

    override fun getLayoutId(): Int = R.layout.dialog_wish_detail

    override fun onViewCreate(view: View) {
        binding = DialogWishDetailBinding.bind(view)
        binding.llLike.setOnClickListener {
            val wish = wishRecord ?: return@setOnClickListener
            //乐观更新：先翻状态，失败时由 onLikeFailed 回滚
            wish.likedByMe = !wish.likedByMe
            wish.likeCount = (wish.likeCount + if (wish.likedByMe) 1 else -1).coerceAtLeast(0)
            renderLike()
            onToggleLike?.invoke(wish.id)
        }
        binding.tvBlessSubmit.setOnClickListener {
            val content = binding.etBlessContent.text?.toString()?.trim().orEmpty()
            val wish = wishRecord ?: return@setOnClickListener
            if (content.isEmpty()) {
                ToastUtil.showMessage("祝福内容不能为空")
                return@setOnClickListener
            }
            binding.tvBlessSubmit.alpha = 0.6f
            onSubmitBless?.invoke(wish.id, content)
        }
    }

    /**
     * 填充愿望内容
     */
    fun render(wishRecord: WishRecordBean?) {
        ensureViewCreated()
        this.wishRecord = wishRecord
        val wish = wishRecord
        binding.tvWishContent.text = wish?.wishContent.orEmpty()

        val anonymous = wish?.isAnonymous == 1
        //时间：匿名 / 非匿名都展示
        binding.tvWishTime.text = "许于 ${wish?.createDate?.take(10).orEmpty()}"

        if (anonymous) {
            //匿名：不展示任何用户信息（头像 / 昵称 / 等级 / 称号 / 头像框），仅标明匿名用户
            binding.llWisher.visibility = View.GONE
            binding.tvWisherAnonymous.visibility = View.VISIBLE
            renderLike()
            return
        }

        //非匿名：展示头像 + 昵称 + 等级 + 称号（头像框 / 3D 昵称预留）
        binding.llWisher.visibility = View.VISIBLE
        binding.tvWisherAnonymous.visibility = View.GONE

        //头像：无头像用默认头像；loadHead 默认圆形裁剪，所有用户头像统一正圆
        val headUrl = if (wish?.userAvatar.isNullOrEmpty()) null else wish?.userAvatar
        binding.ivWisherAvatar.loadHead(headUrl)

        //头像框：预留，后端下发 avatarFrame 时叠加显示，否则隐藏
        if (!wish?.avatarFrame.isNullOrEmpty()) {
            binding.ivWisherFrame.visibility = View.VISIBLE
            Glide.with(binding.ivWisherFrame).load(wish?.avatarFrame).into(binding.ivWisherFrame)
        } else {
            binding.ivWisherFrame.visibility = View.GONE
        }

        //昵称：服务端 getWishingTree 当前未下发 username 字段，缺省回退「佛友」
        val name = wish?.userName.orEmpty().ifEmpty { "佛友" }
        binding.tvWisherName.text = name
        //昵称 3D 特效：预留，后端下发 nickEffect 时启用
        applyNickEffect(binding.tvWisherName, wish?.nickEffect)

        //等级：后端下发 userLevel>0 时显示
        if ((wish?.userLevel ?: 0) > 0) {
            binding.tvWisherLevel.visibility = View.VISIBLE
            binding.tvWisherLevel.text = "Lv.${wish?.userLevel}"
        } else {
            binding.tvWisherLevel.visibility = View.GONE
        }

        //称号：后端下发 userTitle 时显示
        if (!wish?.userTitle.isNullOrEmpty()) {
            binding.tvWisherTitle.visibility = View.VISIBLE
            binding.tvWisherTitle.text = wish?.userTitle
        } else {
            binding.tvWisherTitle.visibility = View.GONE
        }

        renderLike()
    }

    /**
     * 昵称 3D 特效（预留）：后端下发 nickEffect 时启用立体样式。
     * 当前实现为金色投影营造浮雕立体感；后续可按 nickEffect 类型扩展为自定义字体 / 描边图层。
     */
    private fun applyNickEffect(nameView: TextView, effect: String?) {
        if (effect.isNullOrEmpty()) {
            nameView.setShadowLayer(0f, 0f, 0f, 0)
            return
        }
        nameView.setShadowLayer(
            2.5f, 1.5f, 1.5f,
            ContextCompat.getColor(nameView.context, BaseR.color.c_gold)
        )
    }

    private fun renderLike() {
        val wish = wishRecord ?: return
        binding.tvLikeIcon.text = if (wish.likedByMe) "❤️" else "🤍"
        binding.tvLikeCount.text = wish.likeCount.toString()
    }

    /**
     * 祝福成功
     */
    fun onBlessSuccess() {
        ToastUtil.showMessage("祝愿成功，功德无量")
        dismiss()
    }

    /**
     * 祝福失败（含重复祝愿），提示服务端文案并恢复按钮
     */
    fun onBlessFailed(msg: String?) {
        binding.tvBlessSubmit.alpha = 1f
        ToastUtil.showMessage(msg ?: "祝愿失败，请稍后再试")
    }

    /**
     * 点赞失败，回滚乐观更新
     */
    fun onLikeFailed(msg: String?) {
        val wish = wishRecord ?: return
        wish.likedByMe = !wish.likedByMe
        wish.likeCount = (wish.likeCount + if (wish.likedByMe) 1 else -1).coerceAtLeast(0)
        renderLike()
        ToastUtil.showMessage(msg ?: "点赞失败，请稍后再试")
    }
}

package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import com.fingertip.baselib.bean.WishRecordBean
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
        binding.tvWishContent.text = wishRecord?.wishContent.orEmpty()
        //服务端列表接口未带许愿人资料，统一展示「佛友」
        binding.tvWisher.text = "佛友 许于 ${wishRecord?.createDate?.take(10).orEmpty()}"
        renderLike()
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

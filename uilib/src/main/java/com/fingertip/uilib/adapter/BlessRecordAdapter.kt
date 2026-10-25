package com.fingertip.uilib.adapter

import android.content.Context
import com.fingertip.baselib.bean.BlessRecordBean
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.baselib.util.loadImgNoHolder
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemMyBlessBinding

/**
 * 祝福记录 Adapter，我的祝愿页和愿望祝福列表弹窗共用。
 * 展示许愿人信息 + 愿望内容 + 祝福内容；我的祝愿页多显示等级，
 * 弹窗里许愿人可能是自己，userLevel 不传就不显示
 */
class BlessRecordAdapter(
    context: Context,
    private val showLevel: Boolean = true
) : TopRcAdapter<BlessRecordBean, TopRcAdapter.TopRcViewHolder>(context) {

    override fun initLayoutId(viewType: Int): Int = R.layout.item_my_bless

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemMyBlessBinding>()
        val item = get(position) ?: return

        val wisher = item.wishingAccountInfo
        binding.ivAvatar.loadImgNoHolder(wisher?.avatar)
        binding.tvUserName.text = wisher?.userName ?: "佛友"
        binding.tvUserLevel.text = "lv${wisher?.userLevel ?: 0}"
        binding.tvUserLevel.visibility = if (showLevel) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvBlessDate.text = item.blessDate.orEmpty()
        binding.tvWishContent.text = item.wishContent.orEmpty()
        binding.tvBlessContent.text = item.blessContent.orEmpty()
    }
}

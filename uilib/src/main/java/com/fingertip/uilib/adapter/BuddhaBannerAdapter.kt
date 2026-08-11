package com.fingertip.uilib.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.util.loadImgNoHolder
import com.fingertip.uilib.R
import com.youth.banner.adapter.BannerAdapter

/**
 * 佛像 Banner 适配器
 */
class BuddhaBannerAdapter(
    private val context: Context,
    list: List<BuddhaConfig>
) : BannerAdapter<BuddhaConfig, BuddhaBannerAdapter.BannerVH>(list) {

    override fun onCreateHolder(parent: ViewGroup?, viewType: Int): BannerVH {
        val view = LayoutInflater.from(context).inflate(R.layout.item_buddha_banner, parent, false)
        return BannerVH(view)
    }

    override fun onBindView(holder: BannerVH, data: BuddhaConfig?, position: Int, size: Int) {
        val iv = holder.itemView.findViewById<ImageView>(R.id.iv_banner_buddha)
        iv.loadImgNoHolder(data?.buddhaModuleUrl)
    }

    class BannerVH(view: View) : RecyclerView.ViewHolder(view)
}

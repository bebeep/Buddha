package com.fingertip.uilib.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.BannerEntity
import com.fingertip.baselib.util.loadImg
import com.fingertip.uilib.R
import com.youth.banner.adapter.BannerAdapter
import kotlinx.android.synthetic.main.item_me_banner.view.*


/**
 * 通用的bannerAdapter
 */
class MeBannerAdapter(val context: Context, list:List<BannerEntity>, val onClick:(viewId:Int, pos:Int)->Unit):BannerAdapter<BannerEntity,MeBannerAdapter.BannerViewAdapter>(list) {



    override fun onCreateHolder(parent: ViewGroup?, viewType: Int): BannerViewAdapter {
        val view = LayoutInflater.from(context).inflate(R.layout.item_me_banner, parent, false)
        return BannerViewAdapter(view)
    }

    override fun onBindView(holder: BannerViewAdapter, data: BannerEntity?, position: Int, size: Int) {

        data?.let {
            holder.itemView.iv_banner_content.loadImg(data.imgUrl)
        }

    }


    class BannerViewAdapter(view:View): RecyclerView.ViewHolder(view)



}
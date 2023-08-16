package com.buddha.b_book.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.buddha.b_book.R
import com.youth.banner.adapter.BannerAdapter


/**
 * 通用的bannerAdapter
 */
class FojingReadAdapter(val context: Context, list:List<String>):BannerAdapter<String,FojingReadAdapter.BannerViewAdapter>(list) {



    override fun onCreateHolder(parent: ViewGroup?, viewType: Int): BannerViewAdapter {
        val view = LayoutInflater.from(context).inflate(R.layout.item_fojing_read, parent, false)
        return BannerViewAdapter(view)
    }

    override fun onBindView(holder: BannerViewAdapter, data: String?, position: Int, size: Int) {


    }


    class BannerViewAdapter(view:View): RecyclerView.ViewHolder(view)



}
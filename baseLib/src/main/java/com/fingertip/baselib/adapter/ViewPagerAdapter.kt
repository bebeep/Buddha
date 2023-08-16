package com.fingertip.baselib.adapter

import android.content.Context
import android.view.View
import androidx.viewpager.widget.PagerAdapter
import android.view.ViewGroup
import com.fingertip.baselib.util.loadImg
import com.github.chrisbanes.photoview.PhotoView

class ViewPagerAdapter(
    private val data: MutableList<String?>,
    private val c: Context,
    private val onClickListener: View.OnClickListener
) : PagerAdapter() {
    override fun getCount(): Int {
        return data.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val glideImageView = PhotoView(c)
        glideImageView.setOnClickListener(onClickListener)
        glideImageView.id = position
        glideImageView.loadImg(data[position]?:"",cropImage=false)
        container.addView(glideImageView)
        return glideImageView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as PhotoView)
    }

    fun setData(data: List<String>?) {
        this.data.clear()
        this.data.addAll(data!!)
        notifyDataSetChanged()
    }
}
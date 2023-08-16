package com.fingertip.baselib.dialog

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.fingertip.baseLib.R
import com.fingertip.baselib.adapter.ViewPagerAdapter
import com.fingertip.baselib.top.TopDialog
import kotlinx.android.synthetic.main.item_bigpics.*

/**
 * 查看大图
 */
class BigImageDialog(context: Context, var pics: List<String?> = ArrayList(), var index:Int = 0,val singlePic:String = "") : TopDialog(context){

    var picList = ArrayList<String?>()
    lateinit var adapter: ViewPagerAdapter

    override fun attachLayoutRes(): Int = R.layout.item_bigpics

    override fun onViewInit(view: View?) {
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        picList.clear()
        if (pics.isEmpty() && singlePic.isNotEmpty()) picList.add(singlePic)
        else{
            for (picPath in pics) if (!picPath.isNullOrEmpty())this.picList.add(picPath)
        }

        adapter = ViewPagerAdapter(picList,context) {
            dismiss()
        }
        vp.visibility = View.VISIBLE
        selected_number_tv.visibility = View.VISIBLE
        vp.adapter = adapter
        vp.addOnPageChangeListener(object :ViewPager.OnPageChangeListener{
            override fun onPageScrolled(position: Int,positionOffset: Float,positionOffsetPixels: Int) {
            }
            override fun onPageSelected(position: Int) {
                selected_number_tv.text = "${position+1}/${picList.size}"
            }
            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        vp.currentItem = index
        selected_number_tv.visibility = if (picList.size ==0)View.GONE else View.VISIBLE
        selected_number_tv.text = "${index+1}/${picList.size}"
    }

}
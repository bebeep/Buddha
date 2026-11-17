package com.fingertip.baselib.dialog

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import com.fingertip.baselib.R
import com.fingertip.baselib.adapter.ViewPagerAdapter
import com.fingertip.baselib.top.TopDialog

/**
 * 查看大图
 */
class BigImageDialog(context: Context, var pics: List<String?> = ArrayList(), var index:Int = 0,val singlePic:String = "") : TopDialog(context, R.style.gd_big_image_dialog){

    var picList = ArrayList<String?>()
    lateinit var adapter: ViewPagerAdapter

    override fun attachLayoutRes(): Int = R.layout.item_bigpics

    override fun onViewInit(view: View?) {
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        //铺满全屏：Dialog 默认不出系统栏区域。NO_LIMITS 让窗口延伸到系统栏底下，
        //再把状态栏/导航栏背景透明化，否则系统栏自带的底色会盖在 Dialog 画面上
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window?.statusBarColor = android.graphics.Color.TRANSPARENT
            window?.navigationBarColor = android.graphics.Color.TRANSPARENT
            window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
        val vp = view?.findViewById<ViewPager>(R.id.vp)
        val selected_number_tv = view?.findViewById<android.widget.TextView>(R.id.selected_number_tv)
        picList.clear()
        if (pics.isEmpty() && singlePic.isNotEmpty()) picList.add(singlePic)
        else{
            for (picPath in pics) if (!picPath.isNullOrEmpty())this.picList.add(picPath)
        }

        adapter = ViewPagerAdapter(picList,context) {
            dismiss()
        }
        vp?.visibility = View.VISIBLE
        selected_number_tv?.visibility = View.VISIBLE
        vp?.adapter = adapter
        vp?.addOnPageChangeListener(object :ViewPager.OnPageChangeListener{
            override fun onPageScrolled(position: Int,positionOffset: Float,positionOffsetPixels: Int) {
            }
            override fun onPageSelected(position: Int) {
                selected_number_tv?.text = "${position+1}/${picList.size}"
            }
            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        vp?.currentItem = index
        selected_number_tv?.visibility = if (picList.size ==0)View.GONE else View.VISIBLE
        selected_number_tv?.text = "${index+1}/${picList.size}"
    }

}
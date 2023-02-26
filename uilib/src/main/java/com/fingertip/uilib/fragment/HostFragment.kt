package com.fingertip.uilib.fragment

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.BannerEntity
import com.fingertip.baselib.bean.HostMenu
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.HostBannerAdapter
import com.fingertip.uilib.adapter.HostMarqueeAdapter
import com.fingertip.uilib.adapter.HostMenuAdapter
import com.fingertip.uilib.dialog.SignInDialog
import com.youth.banner.Banner
import kotlinx.android.synthetic.main.fragment_host.*
import org.greenrobot.eventbus.Subscribe

/**
 * 首页
 */
class HostFragment : TopFragment() {
    override fun layoutId(): Int = R.layout.fragment_host

    override fun initShiTu() {

        initMenu()
        initBanner()
        initMarqueeView()
    }



    lateinit var adapter:HostMenuAdapter
    private fun initMenu(){
        rc_menu.layoutManager = GridLayoutManager(requireContext(),3)
        adapter = HostMenuAdapter(requireContext()){ pos->
            when(pos){
                0->{}
                1->{}
                2->{}
                3->{}
                4->{}
                5->{}
            }
        }
        rc_menu.adapter = adapter
        adapter.initData(listOf(
            HostMenu("供佛",R.mipmap.icon_menu_gongfo,R.drawable.bg_host_menu),
            HostMenu("佛经",R.mipmap.icon_menu_fojing,R.drawable.bg_host_menu),
            HostMenu("功德",R.mipmap.icon_menu_gongde,R.drawable.bg_host_menu),
            HostMenu("祈福",R.mipmap.icon_menu_qifu,R.drawable.bg_host_menu),
            HostMenu("许愿",R.mipmap.icon_menu_xuyuan,R.drawable.bg_host_menu),
            HostMenu("放生",R.mipmap.icon_menu_fangsheng,R.drawable.bg_host_menu)
        ))
    }



    var bannerAdapter : HostBannerAdapter?=null
    var bannerList =  ArrayList<BannerEntity>()
    private fun initBanner(){
        bannerList.add(BannerEntity().apply { imgUrl =  "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b"})
        bannerList.add(BannerEntity().apply { imgUrl =  "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b"})
        bannerList.add(BannerEntity().apply { imgUrl =  "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b"})
        bannerAdapter = HostBannerAdapter(requireContext(),bannerList){ viewId, pos ->

        }
        (banner as Banner<BannerEntity, HostBannerAdapter>)?.setAdapter(bannerAdapter)
        banner.visibility = if (bannerList.isNullOrEmpty()) View.GONE else View.VISIBLE
        banner.setIndicator(indicator,false)
    }


    val marqueeList = ArrayList<String>()
    lateinit var marqueeAdapter:HostMarqueeAdapter
    private fun initMarqueeView(){
        marqueeList.add("")
        marqueeList.add("")
        marqueeList.add("")
        marqueeAdapter = HostMarqueeAdapter(marqueeList)
        v_marquee.setAdapter(marqueeAdapter)


//        tv_sign.setOnClickListener {
//            marqueeList.clear()
//            marqueeList.add(Math.random().toInt().toString())
//            marqueeAdapter.setData(marqueeList)
//        }
    }


    override fun getClickViews() = listOf(tv_sign,iv_baifo,iv_jihuai,cl_moment)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v){
            tv_sign->{//签到
                log(value="签到")
                SignInDialog(requireContext()).show("每日签到领取----")
            }
            iv_baifo->{//拜佛

            }
            iv_jihuai->{//祭怀

            }
            cl_moment->{//查看所有动态

            }
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        v_marquee.startFlipping()
    }

    override fun onSupportInvisible() {
        super.onSupportInvisible()
        v_marquee.stopFlipping()
    }

    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when(event.what) {

        }
    }
}
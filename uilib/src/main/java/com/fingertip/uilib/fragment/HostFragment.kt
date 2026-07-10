package com.fingertip.uilib.fragment

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.buddha.b_book.fragment.BuddhaTextsFragment
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
import com.fingertip.uilib.fragment.worshiping.GongFoFragment
import com.youth.banner.Banner
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
        requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rc_menu).layoutManager = GridLayoutManager(requireContext(),3)
        adapter = HostMenuAdapter(requireContext()){ pos->
            when(pos){
                0->{//供佛
                    (parentFragment as? MainFragment)?.start(GongFoFragment())
                }
                1->{//佛经
                    (parentFragment as? MainFragment)?.start(BuddhaTextsFragment())
                }
                2->{//功德

                }
                3->{//祈福

                }
                4->{//许愿

                }
                5->{//放生

                }
            }
        }
        requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rc_menu).adapter = adapter
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
        val bannerView = requireView().findViewById<View>(R.id.banner)
        (bannerView as? Banner<BannerEntity, HostBannerAdapter>)?.setAdapter(bannerAdapter)
        bannerView.visibility = if (bannerList.isNullOrEmpty()) View.GONE else View.VISIBLE
        (bannerView as? Banner<BannerEntity, HostBannerAdapter>)?.setIndicator(requireView().findViewById(R.id.indicator),false)
    }


    val marqueeList = ArrayList<String>()
    lateinit var marqueeAdapter:HostMarqueeAdapter
    private fun initMarqueeView(){
        marqueeList.add("")
        marqueeList.add("")
        marqueeList.add("")
        marqueeAdapter = HostMarqueeAdapter(marqueeList)
        requireView().findViewById<com.stx.xmarqueeview.XMarqueeView>(R.id.v_marquee).setAdapter(marqueeAdapter)


//        tv_sign.setOnClickListener {
//            marqueeList.clear()
//            marqueeList.add(Math.random().toInt().toString())
//            marqueeAdapter.setData(marqueeList)
//        }
    }


    override fun getClickViews() = listOf(
        requireView().findViewById<View>(R.id.tv_sign),
        requireView().findViewById<View>(R.id.iv_baifo),
        requireView().findViewById<View>(R.id.iv_jihuai),
        requireView().findViewById<View>(R.id.cl_moment)
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v){
            requireView().findViewById<View>(R.id.tv_sign)->{//签到
                log(value="签到")
                SignInDialog(requireContext()).show("每日签到领取----")
            }
            requireView().findViewById<View>(R.id.iv_baifo)->{//拜佛

            }
            requireView().findViewById<View>(R.id.iv_jihuai)->{//祭怀

            }
            requireView().findViewById<View>(R.id.cl_moment)->{//查看所有动态

            }
        }
    }

    override fun onVisible() {
        super.onVisible()
        requireView().findViewById<com.stx.xmarqueeview.XMarqueeView>(R.id.v_marquee).startFlipping()
    }

    override fun onInvisible() {
        super.onInvisible()
        requireView().findViewById<com.stx.xmarqueeview.XMarqueeView>(R.id.v_marquee).stopFlipping()
    }

    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when(event.what) {

        }
    }
}
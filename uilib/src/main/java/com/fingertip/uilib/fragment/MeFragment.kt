package com.fingertip.uilib.fragment

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.fingertip.baselib.bean.BannerEntity
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.PicUtils
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MeBannerAdapter
import com.fingertip.uilib.viewmodel.MeFragmentVM
import com.youth.banner.Banner
import kotlinx.android.synthetic.main.fragment_me.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe

class MeFragment : TopPmFragment<MeFragmentVM>(), PicUtils.UpPicCallBack {
    override fun layoutId(): Int = R.layout.fragment_me
    override fun initVM(): MeFragmentVM = provideVM()

    override fun initShiTu() {
        picUtils.upPicCallBack = this


        initBanner()

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            nsl.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//                val slideOffset = min(scrollY * 1.0f / 88.toPx(),1f)
//                cl_title.setBackgroundColor(ColorUtil.changeAlpha(resources.getColor(R.color.white),slideOffset))
//            }
//        }
    }


    var bannerAdapter : MeBannerAdapter?=null
    var bannerList =  ArrayList<BannerEntity>()
    private fun initBanner(){
        bannerList.add(BannerEntity().apply { imgUrl =  "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b"})
        bannerList.add(BannerEntity().apply { imgUrl =  "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b"})
        bannerList.add(BannerEntity().apply { imgUrl =  "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b"})
        bannerAdapter = MeBannerAdapter(requireContext(),bannerList){ viewId, pos ->

        }
        (banner as Banner<BannerEntity, MeBannerAdapter>)?.setAdapter(bannerAdapter)
        banner.visibility = if (bannerList.isNullOrEmpty()) View.GONE else View.VISIBLE
        banner.setIndicator(indicator,false)
    }


    override fun getClickViews(): List<View> = mutableListOf(iv_setting, iv_edit)

    override fun onSingleClick(v: View?) {
        when(v){
            iv_setting->{

            }

            iv_edit -> {
//                startActRootFragment(EditFragment())
            }

        }
    }







    override fun initObserver() {
        super.initObserver()

        mViewModel.setAvatarResult.observe(this) {

        }
    }

    override fun callback(isSuccess: Boolean, outfile: String?, t: Throwable?) {
        super.callback(isSuccess, outfile, t)
        outfile?.let {
            picUtils.uploadFile(it)
        }
    }

    override fun start() {
        lifecycleScope.launch(Dispatchers.Main) {
            startWaiting()
        }
    }

    override fun isOktoUp(avatarPath: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            loadEnding()
            log(fName, "上传头像")
            mViewModel.upAvatar(avatarPath)
        }
    }

    override fun errorend() {
        lifecycleScope.launch(Dispatchers.Main) {
            loadEnding()
        }
    }

    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when(event.what) {

        }
    }
}
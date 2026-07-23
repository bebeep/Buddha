package com.fingertip.uilib.fragment

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.fingertip.baselib.bean.BannerEntity
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ColorUtil
import com.fingertip.baselib.util.PicUtils
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MeBannerAdapter
import com.fingertip.uilib.databinding.FragmentMeBinding
import com.fingertip.uilib.viewmodel.MeFragmentVM
import com.lzlz.toplib.extention.toPx
import com.youth.banner.Banner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import kotlin.math.min

class MeFragment : TopPmFragment<MeFragmentVM>(){
    override fun layoutId(): Int = R.layout.fragment_me
    override fun initVM(): MeFragmentVM = provideVM()

    private val binding get() = mBinding as FragmentMeBinding

    override fun initShiTu() {
        initBanner()

        binding.nsl.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val slideOffset = min(scrollY * 1.0f / 88.toPx(),1f)
            binding.clTitle.setBackgroundColor(ColorUtil.changeAlpha(resources.getColor(R.color.white),slideOffset))
        }
    }


    var bannerAdapter : MeBannerAdapter?=null
    var bannerList =  ArrayList<BannerEntity>()
    private fun initBanner(){
        bannerList.add(BannerEntity().apply { imgUrl =  "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b"})
        bannerList.add(BannerEntity().apply { imgUrl =  "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b"})
        bannerList.add(BannerEntity().apply { imgUrl =  "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b"})
        bannerAdapter = MeBannerAdapter(requireContext(),bannerList){ viewId, pos ->

        }
        val bannerView = binding.banner
        (bannerView as? Banner<BannerEntity, MeBannerAdapter>)?.setAdapter(bannerAdapter)
        bannerView.visibility = if (bannerList.isNullOrEmpty()) View.GONE else View.VISIBLE
        (bannerView as? Banner<BannerEntity, MeBannerAdapter>)?.setIndicator(binding.indicator,false)
    }


    override fun getClickViews(): List<View> = mutableListOf(binding.ivSetting, binding.ivEdit)

    override fun onSingleClick(v: View?) {
        when(v){
            binding.ivSetting->{

            }

            binding.ivEdit -> {
//                startActRootFragment(EditFragment())
            }

        }
    }







    override fun initObserver() {
        super.initObserver()

        mViewModel.setAvatarResult.observe(this) {

        }
    }
    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when(event.what) {

        }
    }
}
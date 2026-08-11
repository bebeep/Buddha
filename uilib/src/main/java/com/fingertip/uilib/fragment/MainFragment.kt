package com.fingertip.uilib.fragment

import android.view.View
import com.alibaba.sdk.android.oss.common.auth.OSSFederationCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.PicUtils
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentMainBinding
import com.fingertip.uilib.fragment.moment.MomentFragment
import com.fingertip.uilib.fragment.worshiping.WorshipFragment
import com.fingertip.uilib.viewmodel.MainVM
import com.fingertip.uilib.widgets.bottom_menu.BottomMenu
import com.fingertip.uilib.widgets.bottom_menu.BottomMenuItem
import com.lzlz.toplib.extention.gone
import com.lzlz.toplib.extention.visible
import com.weikaiyun.fragmentation.SupportFragment
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe

import com.fingertip.uilib.fragment.HostFragment
import com.fingertip.uilib.fragment.RankFragment
import com.fingertip.uilib.fragment.MeFragment

/**
 * 主Fragment
 */
class MainFragment : TopVMFragment<MainVM>(), BottomMenu.MenuSelectCallback {

    override fun initVM() = MainVM()

    override fun layoutId() = R.layout.fragment_main

    private val binding get() = mBinding as FragmentMainBinding

    private val fragmentList = ArrayList<SupportFragment>()
    private var lastFragment: SupportFragment? = null

    override fun initShiTu() {

        if (findChildFragment(HostFragment::class.java) != null) {
            fragmentList.add(findChildFragment(HostFragment::class.java))
            fragmentList.add(findChildFragment(MomentFragment::class.java))
            fragmentList.add(findChildFragment(WorshipFragment::class.java))
            fragmentList.add(findChildFragment(RankFragment::class.java))
            fragmentList.add(findChildFragment(MeFragment::class.java))
        } else {
            fragmentList.add(HostFragment())
            fragmentList.add(MomentFragment())
            fragmentList.add(WorshipFragment())
            fragmentList.add(RankFragment())
            fragmentList.add(MeFragment())

            loadMultipleRootFragment(R.id.multi_container, 0, *fragmentList.toTypedArray())
        }

        lastFragment = fragmentList[0]

        binding.bottomMenu.also {
            it.menuSelectCallback = this
            it.addMenuItem(BottomMenuItem(R.mipmap.icon_menu, true))
            it.addMenuItem(BottomMenuItem(R.mipmap.icon_menu))
            it.addMenuItem(BottomMenuItem(R.mipmap.icon_menu))
            it.addMenuItem(BottomMenuItem(R.mipmap.icon_menu, hasMsg = true))
            it.addMenuItem(BottomMenuItem(R.mipmap.icon_menu))
        }


        initOss()
    }


    override fun initObserver() {

    }


    /**
     * 初始阿里云OSS
     */
    private fun initOss()
    {
        val credentialProvider = object : OSSFederationCredentialProvider()
        {
            override fun getFederationToken(): OSSFederationToken? {
                // OSS SDK 在后台线程调用，可安全使用 runBlocking 同步获取
                val ossConfig = runBlocking { mViewModel.getOssConfigSync() }
                if (ossConfig != null)
                {
                    GlobalConfig.globalParam?.ossFileEndPoint = ossConfig.endPoint
                    GlobalConfig.globalParam?.bucketName = ossConfig.bucketName
                    return OSSFederationToken(
                        ossConfig.stsAccessKeyId,
                        ossConfig.stsAccessKeySecret,
                        ossConfig.stsSecurityToken,
                        System.currentTimeMillis() + 3600 * 1000
                    )
                }
                return null
            }
        }
        PicUtils.initOssWithSts(GlobalConfig.globalParam?.ossFileEndPoint ?: "", credentialProvider)
    }



    var lastIndex = 0
    override fun onMenuSelect(pos: Int) {
        showHideFragment(fragmentList[pos], lastFragment)
        lastFragment = fragmentList[pos]
        lastIndex = pos
        if (pos == 2) {
//            binding.multiContainer.gone()
//            binding.bottomMenu.alpha = 0.2f
        } else {
//            binding.multiContainer.visible()
//            binding.bottomMenu.alpha = 1f
        }
    }


    @Subscribe
    fun onMessageEvent(event: MessageEvent) {

    }
}
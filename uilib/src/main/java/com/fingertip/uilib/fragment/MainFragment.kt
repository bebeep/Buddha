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
import com.fingertip.uilib.dialog.ExitConfirmDialog
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
import com.fingertip.uilib.fragment.MeFragment

/**
 * 主Fragment
 *
 * 海外版信息架构（2026-11 调整）：
 *   首页 / 拜佛(含供灯) / 佛友圈 / 我的 —— 共 4 个 tab
 * 排行榜（RankFragment）整体下线；游戏（放生/禅院）不做 tab，收进首页游戏卡区。
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
            fragmentList.add(findChildFragment(WorshipFragment::class.java))
            fragmentList.add(findChildFragment(MomentFragment::class.java))
            fragmentList.add(findChildFragment(MeFragment::class.java))
        } else {
            // 顺序必须与底部菜单一致：首页 / 拜佛 / 佛友圈 / 我的
            fragmentList.add(HostFragment())
            fragmentList.add(WorshipFragment())
            fragmentList.add(MomentFragment())
            fragmentList.add(MeFragment())

            loadMultipleRootFragment(R.id.multi_container, 0, *fragmentList.toTypedArray())
        }

        lastFragment = fragmentList[0]

        binding.bottomMenu.also {
            it.menuSelectCallback = this
            // 4 个 tab 图标兜底（tabBarConfig 未下发时使用），顺序同 fragmentList
            it.addMenuItem(BottomMenuItem(com.fingertip.baselib.R.drawable.icon_tab_host_u, true))
            it.addMenuItem(BottomMenuItem(com.fingertip.baselib.R.drawable.icon_tab_baifo_u))
            // 佛友圈带未读红点（原第 4 位消息角标平移到第 3 位）
            it.addMenuItem(BottomMenuItem(com.fingertip.baselib.R.drawable.icon_tab_post_u, hasMsg = true))
            it.addMenuItem(BottomMenuItem(com.fingertip.baselib.R.drawable.icon_tab_me_u))
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



    /**
     * 供子页面（首页各区块）切换底部 tab。
     *
     * 只暴露这一个入口，内部仍走 BottomMenu.selectTab → 子 View 的 performClick，
     * 这样选中态、onMenuSelect 回调和 lastIndex 都由现有逻辑统一处理，
     * 子页面不需要也不应该去 findViewById 拿 BottomMenu。
     *
     * @param index 0首页 1拜佛 2佛友圈 3我的
     */
    fun selectTab(index: Int) {
        binding.bottomMenu.selectTab(index)
    }

    var lastIndex = 0
    override fun onMenuSelect(pos: Int) {        showHideFragment(fragmentList[pos], lastFragment)
        lastFragment = fragmentList[pos]
        lastIndex = pos
        // 拜佛 tab 现在是第 2 位（index=1），原 index=2 的特殊处理保留但已无匹配
        if (pos == 1) {
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

    //------------------------- 返回键 -------------------------

    private var exitDialog: ExitConfirmDialog? = null

    /**
     * 4 个 tab 都是 multi_container 里的根 Fragment（canPop = false），
     * 所以返回键会一路冒泡到这里（ContainerActivity 的 onBackPressedSupport 只管二级页面）。
     *
     * 规则：
     * - 不在首页 → 切回首页，不退出
     * - 已在首页 → 弹确认框，确认后才退出
     */
    override fun onBackPressedSupport(): Boolean {
        val dialog = exitDialog
        if (dialog != null && dialog.isShowing) {
            //确认框开着的时候，返回键只关弹窗
            dialog.dismiss()
            return true
        }
        if (lastIndex != 0) {
            binding.bottomMenu.selectTab(0)
            return true
        }
        showExitConfirm()
        return true
    }

    private fun showExitConfirm() {
        if (exitDialog?.isShowing == true) return
        val ctx = context ?: return
        if (exitDialog == null) {
            exitDialog = ExitConfirmDialog(ctx) {
                activity?.finishAfterTransition()
            }
        }
        exitDialog?.show()
    }

    override fun onDestroyView() {
        exitDialog?.dismiss()
        exitDialog = null
        super.onDestroyView()
    }
}
package com.fingertip.uilib.fragment

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.uilib.fragment.book.BuddhaTextsFragment
import com.fingertip.baselib.bean.BannerEntity
import com.fingertip.baselib.bean.HostMenu
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.HostBannerAdapter
import com.fingertip.uilib.adapter.HostMarqueeAdapter
import com.fingertip.uilib.adapter.HostMenuAdapter
import com.fingertip.uilib.databinding.FragmentHostBinding
import com.fingertip.uilib.dialog.SignInDialog
import com.fingertip.uilib.fragment.game.FangShengFragment
import com.fingertip.uilib.fragment.worshiping.GongFoFragment
import com.fingertip.uilib.viewmodel.SignInVM
import com.youth.banner.Banner
import org.greenrobot.eventbus.Subscribe

/**
 * 首页
 */
class HostFragment : TopVMFragment<SignInVM>() {

    private val binding get() = mBinding as FragmentHostBinding

    override fun layoutId() = R.layout.fragment_host

    override fun initVM() = SignInVM()

    //签到弹窗，数据回来后由这里转给它
    private var signInDialog: SignInDialog? = null

    override fun initObserver() {
        super.initObserver()

        mViewModel.signInDataResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                signInDialog?.render(result.data)
            } else {
                signInDialog?.showLoadError()
            }
        }

        mViewModel.signInResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                signInDialog?.onSignInSuccess()
                //连签天数和下一档都变了，重新拉一次
                mViewModel.loadSignInData()
            } else {
                signInDialog?.onSignInFailed(result.errorCode)
            }
        }
    }

    /**
     * 打开签到弹窗。先弹出来显示加载态，数据回来再填
     */
    private fun showSignInDialog() {
        val dialog = SignInDialog(requireContext())
        dialog.onSignIn = { mViewModel.signIn() }
        dialog.onReload = { mViewModel.loadSignInData() }
        dialog.setOnDismissListener { signInDialog = null }
        signInDialog = dialog
        dialog.show()
        mViewModel.loadSignInData()
    }

    override fun initShiTu() {

        initMenu()
        initBanner()
        initMarqueeView()
    }



    lateinit var adapter: HostMenuAdapter
    private fun initMenu() {
        binding.rcMenu.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = HostMenuAdapter(requireContext()) { pos ->
            when (pos) {
                0 -> {//供佛
                    (parentFragment as? MainFragment)?.start(GongFoFragment())
                }
                1 -> {//佛经
                    (parentFragment as? MainFragment)?.start(BuddhaTextsFragment())
                }
                2 -> {//功德

                }
                3 -> {//祈福

                }
                4 -> {//许愿

                }
                5 -> {//放生
                    (parentFragment as? MainFragment)?.start(FangShengFragment())
                }
            }
        }
        binding.rcMenu.adapter = adapter
        adapter.initData(listOf(
            HostMenu("供佛", R.mipmap.icon_menu_gongfo, R.drawable.bg_host_menu),
            HostMenu("佛经", R.mipmap.icon_menu_fojing, R.drawable.bg_host_menu),
            HostMenu("功德", R.mipmap.icon_menu_gongde, R.drawable.bg_host_menu),
            HostMenu("祈福", R.mipmap.icon_menu_qifu, R.drawable.bg_host_menu),
            HostMenu("许愿", R.mipmap.icon_menu_xuyuan, R.drawable.bg_host_menu),
            HostMenu("放生", R.mipmap.icon_menu_fangsheng, R.drawable.bg_host_menu)
        ))
    }



    var bannerAdapter: HostBannerAdapter? = null
    var bannerList = ArrayList<BannerEntity>()
    private fun initBanner() {
        bannerList.add(BannerEntity().apply { imgUrl = "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b" })
        bannerList.add(BannerEntity().apply { imgUrl = "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b" })
        bannerList.add(BannerEntity().apply { imgUrl = "https://pics1.baidu.com/feed/d62a6059252dd42a73c6e9397669febec8eab837.jpeg@f_auto?token=75b8e8ee466dbf0e56f425b47d7af74b" })
        bannerAdapter = HostBannerAdapter(requireContext(), bannerList) { viewId, pos ->

        }
        val bannerView = binding.banner
        (bannerView as? Banner<BannerEntity, HostBannerAdapter>)?.setAdapter(bannerAdapter)
        bannerView.visibility = if (bannerList.isNullOrEmpty()) View.GONE else View.VISIBLE
        (bannerView as? Banner<BannerEntity, HostBannerAdapter>)?.setIndicator(binding.indicator, false)
    }


    val marqueeList = ArrayList<String>()
    lateinit var marqueeAdapter: HostMarqueeAdapter
    private fun initMarqueeView() {
        marqueeList.add("")
        marqueeList.add("")
        marqueeList.add("")
        marqueeAdapter = HostMarqueeAdapter(marqueeList)
        binding.vMarquee.setAdapter(marqueeAdapter)


//        tv_sign.setOnClickListener {
//            marqueeList.clear()
//            marqueeList.add(Math.random().toInt().toString())
//            marqueeAdapter.setData(marqueeList)
//        }
    }


    override fun getClickViews() = listOf(
        binding.tvSign,
        binding.ivBaifo,
        binding.ivJihuai,
        binding.clMoment
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.tvSign -> {//签到
                log(value = "签到")
                showSignInDialog()
            }
            binding.ivBaifo -> {//拜佛

            }
            binding.ivJihuai -> {//祭怀

            }
            binding.clMoment -> {//查看所有动态

            }
        }
    }

    override fun onVisible() {
        super.onVisible()
        binding.vMarquee.startFlipping()
    }

    override fun onInvisible() {
        super.onInvisible()
        binding.vMarquee.stopFlipping()
    }

    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when(event.what) {

        }
    }
}
package com.fingertip.uilib.fragment

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.uilib.fragment.book.BuddhaTextsFragment
import com.fingertip.uilib.fragment.blessing.BlessingFragment
import com.fingertip.uilib.fragment.worshiping.DonateFragment
import com.fingertip.baselib.bean.BannerEntity
import com.fingertip.baselib.bean.HostMenu
import com.fingertip.baselib.bean.TodayBuddhaInfo
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
import com.fingertip.uilib.fragment.worshiping.SignInFragment
import com.fingertip.uilib.fragment.wishing.WishTreeFragment
import com.fingertip.uilib.viewmodel.SignInVM
import com.youth.banner.Banner
import org.greenrobot.eventbus.Subscribe
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

        mViewModel.todayBuddhaResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                renderTodayBuddha(result.data)
            }
        }

        mViewModel.signInDataResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                signInDialog?.render(result.data)
            } else {
                signInDialog?.showLoadError(result.errorMsg)
            }
        }

        mViewModel.signInResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                signInDialog?.onSignInSuccess()
                //连签天数和下一档都变了，重新拉一次
                mViewModel.loadSignInData()
            } else {
                signInDialog?.onSignInFailed(result.errorCode, result.errorMsg)
            }
        }
    }

    /**
     * 顶部今日佛历文案。
     *
     * 接口已把 佛历节日 > 传统节日 > 节气 > 斋日 > 农历 的降级做完了，
     * 这里拿到什么显示什么；万一是空的再退回本地公历，避免顶部塌成一条
     */
    private fun renderTodayBuddha(info: TodayBuddhaInfo?) {
        val dateText = info?.dateText
        binding.tvDate.text = if (dateText.isNullOrBlank()) localDateText() else dateText

        val moodText = info?.moodText
        binding.tvMood.text = moodText.orEmpty()
        //用 INVISIBLE 占位，免得数据回来顶部高度跳一下
        binding.tvMood.visibility = if (moodText.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
    }

    //没网时的兜底：佛历年份 + 公历日期，农历算不了就不硬编
    private fun localDateText(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR) + 543
        return "佛历 $year 年  ${SimpleDateFormat("M月d日", Locale.getDefault()).format(Date())}"
    }

    /**
     * 今日佛历一天拉一次即可，回到前台跨天了才会重新请求
     */
    private fun loadTodayBuddhaIfNeed() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        if (todayBuddhaLoadedDate == today) return
        todayBuddhaLoadedDate = today
        mViewModel.loadTodayBuddhaInfo()
    }

    //本次已请求过的日期，yyyyMMdd
    private var todayBuddhaLoadedDate: String? = null

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

        //先铺本地公历，接口回来再覆盖，避免顶部空一截
        binding.tvDate.text = localDateText()
        binding.tvMood.visibility = View.INVISIBLE
        loadTodayBuddhaIfNeed()
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
                    (parentFragment as? MainFragment)?.start(DonateFragment())
                }
                3 -> {//祈福
                    (parentFragment as? MainFragment)?.start(BlessingFragment())
                }
                4 -> {//许愿
                    (parentFragment as? MainFragment)?.start(WishTreeFragment())
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
                (parentFragment as? MainFragment)?.start(SignInFragment())
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
        //从别的页面切回来可能已经跨天，跨天才重新拉
        loadTodayBuddhaIfNeed()
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
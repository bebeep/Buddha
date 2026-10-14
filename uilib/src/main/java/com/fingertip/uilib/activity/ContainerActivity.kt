package com.fingertip.uilib.activity

import com.fingertip.baselib.top.TopActivity
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ActivityContainerBinding
import com.fingertip.uilib.fragment.MainFragment
import com.photo.picker.GalleryPickerOption

/**
 * 主activity
 */
class ContainerActivity : TopActivity() {

    private lateinit var binding: ActivityContainerBinding

    override fun isFullTopBar() = true

    override fun initShiTu() {
        binding = ActivityContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadRootFragment(R.id.root_container, MainFragment())
        initGalleryPicker()
    }

    /**
     * 返回处理。
     *
     * 该 fork(SFragmentation 1.8.4) 默认的 onBackPressedSupport() 是按
     * Fragment 上的 canPop / startByFragmentation 标记来统计的：一旦容器里
     * 找不到「可 pop」的 Fragment，就直接 finishAfterTransition() 退出应用。
     * 二级页面(如 BuddhaTextsFragment、GongFoFragment)是挂在 MainFragment 之上
     * 的普通栈页，返回时应回到 MainFragment，而不是退出。
     * 这里改为：容器里还有上一层就 pop，只有只剩根页面时才退出。
     */
    override fun onBackPressedSupport() {
        if (supportFragmentManager.fragments.size > 1) {
            pop()
        } else {
            super.onBackPressedSupport()
        }
    }


    // 初始化图片选择器
    private fun initGalleryPicker() {
        GalleryPickerOption.setColorPrimary(R.color.purple_200) // Set app primary color
            .setTextColorPrimary(R.color.white) // Set text color for app primary color
            .maxItems(9) // Set max selection count: 1 for single, >1 for multiple
            .isCompress(true) // Default image compression
            .ignoreSize(100)  // Do not compress files smaller than 100kb
            .quality(75)  // Compression quality, default 75%
            .isCrop(true) // Whether to crop image, default false
            .maxVideoSize(15) // Max video file size, default 15Mb
            .debug(false)  // Debug logging
    }






}
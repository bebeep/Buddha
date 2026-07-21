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
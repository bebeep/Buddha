package com.fingertip.uilib.activity

import com.fingertip.baselib.top.TopActivity
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ActivityContainerBinding
import com.fingertip.uilib.fragment.MainFragment

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
    }








}
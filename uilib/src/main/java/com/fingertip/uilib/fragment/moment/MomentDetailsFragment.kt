package com.fingertip.uilib.fragment.moment

import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.MomentVM

class MomentDetailsFragment: TopPmFragment<MomentVM>() {
    override fun initVM() = MomentVM ()

    override fun layoutId() = R.layout.fragment_moment_details

    override fun initShiTu() {

    }
}
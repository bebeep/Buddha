package com.fingertip.uilib.fragment.game

import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.FangShengVM

class FangShengFragment: TopPmFragment<FangShengVM>() {

    override fun initVM(): FangShengVM = provideVM()

    override fun layoutId() = R.layout.fragment_fangsheng

    override fun initShiTu() {

    }
}

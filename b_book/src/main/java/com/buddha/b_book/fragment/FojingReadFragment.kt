package com.buddha.b_book.fragment

import com.buddha.b_book.R
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment

class FojingReadFragment : TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing_copy
    override fun initVM() = BookshelfVM()

    override fun initShiTu() {
        TODO("Not yet implemented")
    }
}
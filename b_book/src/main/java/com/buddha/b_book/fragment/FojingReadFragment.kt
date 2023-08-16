package com.buddha.b_book.fragment

import android.view.View
import com.buddha.b_book.R
import com.buddha.b_book.adapter.FojingReadAdapter
import com.buddha.b_book.dialog.FojingCopySettingDialog
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.youth.banner.Banner
import com.youth.banner.transformer.*
import kotlinx.android.synthetic.main.frag_fojing_read.*

class FojingReadFragment : TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing_read
    override fun initVM() = BookshelfVM()

    override fun initShiTu() {

        initAdapter()

    }


    override fun getClickViews() = listOf(v_back,iv_setting)
    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v){
            v_back->pop()
            iv_setting->{
                FojingCopySettingDialog(requireContext()).show()
            }
        }
    }

    var adapter: FojingReadAdapter?=null
    var list = ArrayList<String>()
    private fun initAdapter(){
        list.add("")
        list.add("")
        list.add("")
        list.add("")
        list.add("")
        adapter = FojingReadAdapter(requireContext(),list)
        (banner_fojing as Banner<String, FojingReadAdapter>).setAdapter(adapter)
        (banner_fojing as Banner<String, FojingReadAdapter>).addPageTransformer(ScaleInTransformer()).addPageTransformer(AlphaPageTransformer())
    }
}
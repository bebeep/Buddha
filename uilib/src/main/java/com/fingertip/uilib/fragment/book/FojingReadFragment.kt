package com.fingertip.uilib.fragment.book

import android.view.View
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FojingReadAdapter
import com.fingertip.uilib.dialog.FojingCopySettingDialog
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.youth.banner.Banner
import com.youth.banner.transformer.*

class FojingReadFragment : TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing_read
    override fun initVM() = BookshelfVM()

    override fun initShiTu() {

        initAdapter()

    }


    override fun getClickViews(): List<View> {
        val v = requireView()
        return listOf(v.findViewById(R.id.v_back), v.findViewById(R.id.iv_setting))
    }
    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v?.id){
            R.id.v_back->pop()
            R.id.iv_setting->{
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
        val bannerView = requireView().findViewById<View>(R.id.banner_fojing)
        (bannerView as Banner<String, FojingReadAdapter>).setAdapter(adapter)
        (bannerView as Banner<String, FojingReadAdapter>).addPageTransformer(ScaleInTransformer()).addPageTransformer(AlphaPageTransformer())
    }
}
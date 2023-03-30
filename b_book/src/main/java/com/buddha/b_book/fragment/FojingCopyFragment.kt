package com.buddha.b_book.fragment

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.buddha.b_book.R
import com.buddha.b_book.adapter.FojingCopyAdapter
import com.buddha.b_book.vm.BookshelfVM
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ToastUtil
import com.lzlz.toplib.extention.onGlobalLayout
import com.lzlz.toplib.extention.toPx
import kotlinx.android.synthetic.main.frag_fojing_copy.*

/**
 * 佛经-抄经
 */
class FojingCopyFragment :TopVMFragment<BookshelfVM>(){
    override fun layoutId() = R.layout.frag_fojing_copy
    override fun initVM() = BookshelfVM()

    override fun initShiTu() {


        initAdapter()

    }


    override fun getClickViews() = listOf(v_back,iv_setting,tv_answer1,tv_answer2,tv_answer3,tv_answer4)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v){
            v_back->pop()
            iv_setting -> ToastUtil.showMessage("设置")
            tv_answer1 -> {
                ToastUtil.showMessage("答案1")
            }
            tv_answer2 -> {
                ToastUtil.showMessage("答案2")
            }
            tv_answer3 -> {
                ToastUtil.showMessage("答案3")
            }
            tv_answer4 -> {
                ToastUtil.showMessage("答案4")
            }
        }
    }

    val list = ArrayList<String>()
    lateinit var adapter: FojingCopyAdapter
    private fun initAdapter(){


        fl_content.onGlobalLayout {

            val row = (fl_content.height - 30.toPx())/85.toPx()
            val column = (fl_content.width-32.toPx())/40.toPx()
            log(value = "fl_content  $row   $column  ")
            val listCount = row*column

            adapter = FojingCopyAdapter(requireContext(),column)
            for (i in 15..listCount) list.add("")
            rv_content.layoutManager = GridLayoutManager(requireContext(),column)
            rv_content.adapter = adapter
            adapter.initData(list)
        }
    }
}
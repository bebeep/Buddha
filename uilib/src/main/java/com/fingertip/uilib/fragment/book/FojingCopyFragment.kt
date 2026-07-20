package com.fingertip.uilib.fragment.book

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FojingCopyAdapter
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.databinding.FragFojingCopyBinding
import com.fingertip.uilib.dialog.FojingCopySettingDialog
import com.lzlz.toplib.extention.onGlobalLayout
import com.lzlz.toplib.extention.toPx

/**
 * 佛经-抄经
 */
class FojingCopyFragment :TopVMFragment<BookshelfVM>(){
    private val binding get() = mBinding as FragFojingCopyBinding
    override fun layoutId() = R.layout.frag_fojing_copy
    override fun initVM() = BookshelfVM()

    override fun initShiTu() {


        initAdapter()

    }


    override fun getClickViews(): List<View> {
        return listOf(binding.vBack.ivBack, binding.ivSetting, binding.tvAnswer1, binding.tvAnswer2, binding.tvAnswer3, binding.tvAnswer4)
    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v?.id){
            R.id.v_back->pop()
            R.id.iv_setting -> {
                FojingCopySettingDialog(requireContext()).show()
            }
            R.id.tv_answer1 -> {
                ToastUtil.showMessage("答案1")
            }
            R.id.tv_answer2 -> {
                ToastUtil.showMessage("答案2")
            }
            R.id.tv_answer3 -> {
                ToastUtil.showMessage("答案3")
            }
            R.id.tv_answer4 -> {
                ToastUtil.showMessage("答案4")
            }
        }
    }

    val list = ArrayList<String>()
    lateinit var adapter: FojingCopyAdapter
    private fun initAdapter(){


        binding.flContent.onGlobalLayout {

            val flContent = binding.flContent
            val row = (flContent.height - 30.toPx())/85.toPx()
            val column = (flContent.width-32.toPx())/40.toPx()
            log(value = "fl_content  $row   $column  ")
            val listCount = row*column

            adapter = FojingCopyAdapter(requireContext(),column)
            for (i in 15..listCount) list.add("")
            binding.rvContent.layoutManager = GridLayoutManager(requireContext(),column)
            binding.rvContent.adapter = adapter
            adapter.initData(list)
        }
    }
}
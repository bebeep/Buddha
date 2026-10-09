package com.fingertip.uilib.fragment.book

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FojingAdapter
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ColorUtil
import com.fingertip.uilib.databinding.FragFojingSubjectDetailsBinding
import com.lzlz.toplib.extention.toPx
import kotlin.math.abs
import kotlin.math.min

/**
 * 佛经-专题详情
 */
class FojingSubjectDetailsFragment :TopVMFragment<BookshelfVM>(){
    private val binding get() = mBinding as FragFojingSubjectDetailsBinding
    override fun layoutId() = R.layout.frag_fojing_subject_details
    override fun initVM() = BookshelfVM()

    var subjectId = 0

    companion object {
        const val SUBJECT_ID = "SUBJECT_ID"
        fun newInstance(subjectId: Int): FojingSubjectDetailsFragment {
            return FojingSubjectDetailsFragment().apply {
                arguments = Bundle().apply {
                    putInt(SUBJECT_ID, subjectId)
                }
            }
        }
    }

    lateinit var adapter: FojingAdapter

    override fun onNewBundle(args: Bundle?) {
        super.onNewBundle(args)
        initShiTu()
    }

    override fun initShiTu() {
        subjectId = arguments?.getInt(SUBJECT_ID, 0) ?: 0
        if (subjectId <= 0) {
            return
        }
        initAdapter()

        binding.appbar.addOnOffsetChangedListener { _, scrollY ->
            val slideOffset = min(abs(scrollY * 1.0f / 88.toPx()), 1f)
            binding.clTitle.setBackgroundColor(
                ColorUtil.changeAlpha(
                    resources.getColor(R.color.white),
                    slideOffset
                )
            )
            binding.tvTitle.alpha = slideOffset
        }

        binding.vBack.ivBack.setOnClickListener {
            pop()
        }

        mViewModel.getBookByTheme(subjectId, 0)
    }


    private fun initAdapter(){
        adapter = FojingAdapter(requireContext())
        binding.recyclerview.layoutManager = GridLayoutManager(requireContext(),3)
        binding.recyclerview.adapter = adapter
    }


    override fun initObserver() {
        super.initObserver()
        mViewModel.bookListResult.observe(this) {
            if (it.success)
            {
                adapter.initData(it.data)
            }
        }
    }
}
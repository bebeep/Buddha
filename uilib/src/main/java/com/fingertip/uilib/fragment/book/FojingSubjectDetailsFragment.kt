package com.fingertip.uilib.fragment.book

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FojingAdapter
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ColorUtil
import com.fingertip.uilib.databinding.FragFojingSubjectDetailsBinding
import com.google.android.material.appbar.AppBarLayout
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

    lateinit var adapter: FojingAdapter

    override fun initShiTu() {
        val v = requireView()

        initAdapter()

        binding.appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, scrollY ->
            val slideOffset = min(abs(scrollY * 1.0f / 88.toPx()),1f)
            binding.clTitle.setBackgroundColor(ColorUtil.changeAlpha(resources.getColor(R.color.white),slideOffset))
            binding.tvTitle.alpha = slideOffset
        })



        binding.vBack.ivBack.setOnClickListener {
            pop()
        }
    }


    private fun initAdapter(){
        adapter = FojingAdapter(requireContext())
        binding.recyclerview.layoutManager = GridLayoutManager(requireContext(),3)
        binding.recyclerview.adapter = adapter

        adapter.initData(listOf("","",""))
    }


}
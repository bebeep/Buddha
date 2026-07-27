package com.fingertip.uilib.fragment.moment

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopFragment
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MomentAdapter
import com.fingertip.uilib.databinding.FragmentMomentChildBinding
import com.fingertip.uilib.fragment.MainFragment
import com.fingertip.uilib.viewmodel.MomentVM

class MomentChildFragment : TopPmFragment<MomentVM>(){
    override fun layoutId(): Int = R.layout.fragment_moment_child

    override fun initVM() = MomentVM()
    private val binding get() = mBinding as FragmentMomentChildBinding

    companion object {
        const val FOLLOW = "FOLLOW" //关注
        const val MOMENT = "MOMENT" //佛友圈
        const val TYPE_STRING = "typeString"
        fun newInstance(typeString: String = MOMENT): MomentChildFragment {
            return MomentChildFragment().apply {
                arguments = Bundle().apply {
                    putString(TYPE_STRING, typeString)

                }
            }
        }
    }

    var adapter: MomentAdapter?=null
    val list = ArrayList<MomentEntity>()
    var pageCount = 0
    var typeString = MOMENT
    override fun initShiTu() {
        typeString = arguments?.getString(TYPE_STRING, MOMENT) ?: MOMENT
        initAdapter()
    }


    private fun initAdapter(){
        adapter = MomentAdapter(requireContext()){
            log(value = "MomentAdapter-----------")
            (parentFragment?.parentFragment as? MainFragment)?.start(MomentDetailsFragment())

        }
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = adapter

        mViewModel.getMomentList(pageCount)
        adapter?.initData(list)
    }


    override fun initObserver() {
        super.initObserver()
        mViewModel.momentListResult.observe(viewLifecycleOwner) {
            log(value = "momentList-----------$it")
            if (it.success)
            {
                if (pageCount == 0) list.clear()
                it.data?.let { it1 ->
                    list.addAll(it1)
                    adapter?.initData(list) ?: log(value = "adapter is null")
                }
            }
        }
    }

}
package com.fingertip.uilib.fragment.message

import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MessageSystemAdapter
import com.fingertip.uilib.databinding.FragmentMessageChildBinding
import com.fingertip.uilib.viewmodel.MainVM

class MessageSystemFragment: TopPmFragment<MainVM>() {
    override fun initVM() = MainVM()
    override fun layoutId() = R.layout.fragment_message_child

    private val binding get() = mBinding as FragmentMessageChildBinding

    lateinit var adapter: MessageSystemAdapter

    override fun initShiTu() {
        initAdapter()
    }



    private fun initAdapter(){
        adapter = MessageSystemAdapter(requireContext()){

        }
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = adapter

        adapter.initData(listOf("","","","","","",""))
    }
}
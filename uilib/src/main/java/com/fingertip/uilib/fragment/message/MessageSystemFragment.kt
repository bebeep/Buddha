package com.fingertip.uilib.fragment.message

import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MessageSystemAdapter
import com.fingertip.uilib.viewmodel.MainVM

class MessageSystemFragment: TopPmFragment<MainVM>() {
    override fun initVM() = MainVM()
    override fun layoutId() = R.layout.fragment_message_child

    lateinit var adapter: MessageSystemAdapter

    override fun initShiTu() {
        initAdapter()
    }



    private fun initAdapter(){
        adapter = MessageSystemAdapter(requireContext()){

        }
        val rv = requireView().findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerview)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        adapter.initData(listOf("","","","","","","",""))
    }
}
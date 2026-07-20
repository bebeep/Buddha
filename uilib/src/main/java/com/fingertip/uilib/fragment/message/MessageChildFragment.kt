package com.fingertip.uilib.fragment.message

import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MessageReplyAdapter
import com.fingertip.uilib.databinding.FragmentMessageChildBinding
import com.fingertip.uilib.viewmodel.MainVM

/**
 * xxx评论了你发布的【发布的内容】:【评论内容】
 * xxx回复了你的评论【自己的评论内容】:【回复内容】
 * xxx发布了【发布的内容】并邀请您浏览
 */
class MessageChildFragment: TopPmFragment<MainVM>() {
    override fun initVM() = MainVM()
    override fun layoutId() = R.layout.fragment_message_child

    private val binding get() = mBinding as FragmentMessageChildBinding

    lateinit var adapter: MessageReplyAdapter


    override fun initShiTu() {
        initAdapter()
    }



    private fun initAdapter(){
        adapter = MessageReplyAdapter(requireContext()){

        }
        val rv = binding.recyclerview
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        adapter.initData(listOf("","","","","","",""))
    }
}
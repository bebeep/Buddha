package com.fingertip.uilib.fragment.message

import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MessageReplyAdapter
import com.fingertip.uilib.viewmodel.MainVM
import kotlinx.android.synthetic.main.fragment_message_child.recyclerview

/**
 * xxx评论了你发布的【发布的内容】:【评论内容】
 * xxx回复了你的评论【自己的评论内容】:【回复内容】
 * xxx发布了【发布的内容】并邀请您浏览
 */
class MessageChildFragment: TopPmFragment<MainVM>() {
    override fun initVM() = MainVM()
    override fun layoutId() = R.layout.fragment_message_child

    lateinit var adapter: MessageReplyAdapter


    override fun initShiTu() {
        initAdapter()
    }



    private fun initAdapter(){
        adapter = MessageReplyAdapter(requireContext()){

        }
        recyclerview.layoutManager = LinearLayoutManager(requireContext())
        recyclerview.adapter = adapter

        adapter.initData(listOf("","","","","","","",""))
    }
}
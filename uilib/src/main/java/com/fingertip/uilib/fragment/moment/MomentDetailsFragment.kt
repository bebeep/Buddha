package com.fingertip.uilib.fragment.moment

import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CommentAdapter
import com.fingertip.uilib.viewmodel.MomentVM
import kotlinx.android.synthetic.main.fragment_moment_details.*

class MomentDetailsFragment: TopPmFragment<MomentVM>() {
    override fun initVM() = MomentVM ()

    override fun layoutId() = R.layout.fragment_moment_details

    override fun initShiTu() {

        initAdapter()
    }


    lateinit var commentAdapter: CommentAdapter
    private fun initAdapter(){
        commentAdapter = CommentAdapter(requireContext()){ position, _, longClickPosition, viewId->

        }

        recyclerview.layoutManager = LinearLayoutManager(requireContext())
        recyclerview.adapter = commentAdapter


        commentAdapter.initData(listOf(CommentEntity(),CommentEntity(),CommentEntity(),CommentEntity(),CommentEntity(),CommentEntity()))
    }
}
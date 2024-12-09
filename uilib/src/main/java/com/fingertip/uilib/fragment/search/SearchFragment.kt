package com.fingertip.uilib.fragment.search

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.view.MyTextView
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MomentAdapter
import com.fingertip.uilib.adapter.SearchHotAdapter
import com.fingertip.uilib.adapter.SearchUserAdapter
import com.fingertip.uilib.viewmodel.MainVM
import kotlinx.android.synthetic.main.fragment_search.*

class SearchFragment:TopPmFragment<MainVM>() {
    override fun initVM() = MainVM()

    override fun layoutId() = R.layout.fragment_search

    lateinit var searchHotAdapter: SearchHotAdapter
    lateinit var searchUserAdapter: SearchUserAdapter
    lateinit var searchMomentAdapter: MomentAdapter

    override fun initShiTu() {
        initAdapter()

        for (i in 0..5) rv_search_history.addView(createTagView("搜索项$i"))




        et_search.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                if (et_search.text.toString().isNotEmpty()){

                }
                et_search.isFocusable = false
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        searchHotAdapter.initData(listOf("成都昭觉寺将于6月初6举行开光仪式","大昭寺万佛归一","大慈寺大年初一万人共拜"))



        searchMomentAdapter.initData(listOf(MomentEntity().apply {
            momentType = 2
            images = ArrayList<String?>().apply {
                add("")
            }
            imageWidths = ArrayList<Int>().apply {
                add(300)
            }
            imageHeights = ArrayList<Int>().apply {
                add(300)
            }
        },MomentEntity().apply {
            momentType = 2
            images = ArrayList<String?>().apply {
                add("")
            }
            imageWidths = ArrayList<Int>().apply {
                add(300)
            }
            imageHeights = ArrayList<Int>().apply {
                add(300)
            }
        },MomentEntity().apply {
            momentType = 2
            images = ArrayList<String?>().apply {
                add("")
            }
            imageWidths = ArrayList<Int>().apply {
                add(300)
            }
            imageHeights = ArrayList<Int>().apply {
                add(300)
            }
        }))


        searchUserAdapter.initData(listOf("","",""))


    }



    private fun initAdapter(){
        searchHotAdapter = SearchHotAdapter(requireContext()){

        }
        rv_search_hot.layoutManager = LinearLayoutManager(requireContext())
        rv_search_hot.adapter = searchHotAdapter



        searchUserAdapter = SearchUserAdapter(requireContext()){

        }
        rv_search_users.layoutManager = LinearLayoutManager(requireContext())
        rv_search_users.adapter = searchUserAdapter


        searchMomentAdapter = MomentAdapter(requireContext()){

        }
        rv_search_moments.layoutManager = LinearLayoutManager(requireContext())
        rv_search_moments.adapter = searchMomentAdapter
    }


    private fun createTagView(tag:String): View {
        val view = View.inflate(requireContext(), R.layout.item_search_history, null)

        view.findViewById<MyTextView>(R.id.tv_search_history_item).text = tag

        return view
    }
}
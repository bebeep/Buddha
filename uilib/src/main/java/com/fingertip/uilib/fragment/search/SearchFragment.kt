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

class SearchFragment:TopPmFragment<MainVM>() {
    override fun initVM() = MainVM()

    override fun layoutId() = R.layout.fragment_search

    lateinit var searchHotAdapter: SearchHotAdapter
    lateinit var searchUserAdapter: SearchUserAdapter
    lateinit var searchMomentAdapter: MomentAdapter

    override fun initShiTu() {
        initAdapter()
        val v = requireView()
        val rvHistory = v.findViewById<android.widget.LinearLayout>(R.id.rv_search_history)
        for (i in 0..5) rvHistory.addView(createTagView("搜索项$i"))


        val etSearch = v.findViewById<com.fingertip.baselib.view.MyEditText>(R.id.et_search)
        etSearch.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                if (etSearch.text.toString().isNotEmpty()){

                }
                etSearch.isFocusable = false
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
        val v = requireView()
        searchHotAdapter = SearchHotAdapter(requireContext()){

        }
        v.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_search_hot).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchHotAdapter
        }



        searchUserAdapter = SearchUserAdapter(requireContext()){

        }
        v.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_search_users).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchUserAdapter
        }


        searchMomentAdapter = MomentAdapter(requireContext()){

        }
        v.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_search_moments).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchMomentAdapter
        }
    }


    private fun createTagView(tag:String): View {
        val view = View.inflate(requireContext(), R.layout.item_search_history, null)

        view.findViewById<MyTextView>(R.id.tv_search_history_item).text = tag

        return view
    }
}
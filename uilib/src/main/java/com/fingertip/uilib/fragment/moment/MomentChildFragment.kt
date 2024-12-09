package com.fingertip.uilib.fragment.moment

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MomentAdapter
import com.fingertip.uilib.fragment.MainFragment
import kotlinx.android.synthetic.main.fragment_moment_child.*

class MomentChildFragment : TopFragment(){
    override fun layoutId(): Int = R.layout.fragment_moment_child
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
        recyclerview.layoutManager = LinearLayoutManager(requireContext())
        recyclerview.adapter = adapter

        list.add(MomentEntity().apply {
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
        })
        list.add(MomentEntity().apply {
            momentType = 2
            images = ArrayList<String?>().apply {
                add("")
                add("")
                add("")
            }
            imageWidths = ArrayList<Int>().apply {
                add(300)
            }
            imageHeights = ArrayList<Int>().apply {
                add(300)
            }
        })
        list.add(MomentEntity().apply {
            momentType = 2
            images = ArrayList<String?>().apply {
                add("")
                add("")
                add("")
                add("")
                add("")
            }
            imageWidths = ArrayList<Int>().apply {
                add(300)
            }
            imageHeights = ArrayList<Int>().apply {
                add(300)
            }
        })
        list.add(MomentEntity().apply {
            momentType = 2
            images = ArrayList<String?>().apply {
                add("")
                add("")
                add("")
                add("")
                add("")
                add("")
                add("")
            }
            imageWidths = ArrayList<Int>().apply {
                add(300)
            }
            imageHeights = ArrayList<Int>().apply {
                add(300)
            }
        })
        adapter?.initData(list)
    }
}
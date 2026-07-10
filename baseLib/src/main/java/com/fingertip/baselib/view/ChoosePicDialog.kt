package com.fingertip.baselib.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.R
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.baselib.top.TopRcAdapter

/**
 * 选择相册/相机dialog
 */
class ChoosePicDialog(
    context: Context,
    var mylist: MutableList<String> = mutableListOf("相机","相册"),
    var onItemClick: (position:Int) -> Unit,
    val colorList: List<String>? = null

) : TopDialogBottomSheetDialog(context) {

    override fun getLayoutId(): Int = R.layout.dialog_choose_pic
    var myAdapter: CZClickAdapter?=null
    override fun onViewCreate(view: View) {
        view.findViewById<View>(R.id.tv_cancel).setOnClickListener {
            dismiss()
        }
        view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerview).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            myAdapter=CZClickAdapter(context,this@ChoosePicDialog)
            adapter =myAdapter
        }
    }

    init {
        myAdapter?.apply {
            initData(mylist)
        }
    }



    inner class CZClickAdapter(context:Context, var dialog: Dialog) :
        TopRcAdapter<String, TopRcAdapter.TopRcViewHolder>(context){


        override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {
            holder.itemView.findViewById<android.widget.TextView>(R.id.tv_titlie).text = get(position)
            colorList?.let {
                holder.itemView.findViewById<android.widget.TextView>(R.id.tv_titlie).setTextColor(Color.parseColor(colorList[position]))
            }
            holder.itemView.findViewById<android.widget.TextView>(R.id.tv_titlie).setOnClickListener {
                dialog.dismiss()
                onItemClick.invoke(position)
            }
        }
        override fun initLayoutId(viewType: Int): Int = R.layout.item_choose_pic
    }
}
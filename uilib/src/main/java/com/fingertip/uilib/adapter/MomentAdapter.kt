package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.dialog.BigImageDialog
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import kotlinx.android.synthetic.main.item_moment.view.*

class MomentAdapter(context: Context,val onItemClick:(entity:MomentEntity)->Unit) : TopRcAdapter<MomentEntity, TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int): Int = R.layout.item_moment

    var bigImageDialog: BigImageDialog?=null

    override fun onBindViewHolder(holder: TopRcViewHolder, position: Int) {

        get(position)?.let {
            holder.itemView.v_divider.visibility = if (position == mlist.size-1) View.GONE else View.VISIBLE
            holder.itemView.rv_photos.visibility = if (it.momentType == 2  && it.images.size > 0) View.VISIBLE else View.GONE
            if (it.momentType == 2  && it.images.size > 0 ){//显示图片集合
                val colums = when(it.images.size){
                    1->1 //单图
                    4->2 //两列
                    else -> 3 //三列
                }
                holder.itemView.rv_photos.layoutManager = GridLayoutManager(context,colums)
                val adapter = MomentImageAdapter(context, it.imageWidths[0],it.imageHeights[0]) { pos->
                    val imageList = ArrayList<String?>()
                    for (image in it.images) if (!image.isNullOrEmpty())imageList.add(image)
                    bigImageDialog = BigImageDialog(context, imageList, pos)
                    bigImageDialog?.show()
                }

                holder.itemView.rv_photos.adapter = adapter
                adapter.initData(it.images)



                holder.itemView.setOnClickListener {_->
                    onItemClick(it)
                }
            }
        }
    }

}
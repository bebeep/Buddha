package com.fingertip.uilib.adapter

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.dialog.BigImageDialog
import com.fingertip.baselib.top.TopRcAdapter
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.ItemMomentBinding

class MomentAdapter(context: Context, val onItemClick: (entity: MomentEntity) -> Unit) : TopRcAdapter<MomentEntity, TopRcAdapter.TopRcViewHolder>(context) {
    override fun initLayoutId(viewType: Int): Int = R.layout.item_moment

    var bigImageDialog: BigImageDialog? = null

    override fun onBindViewHolder(holder: TopRcAdapter.TopRcViewHolder, position: Int) {
        val binding = holder.getBinding<ItemMomentBinding>()
        get(position)?.let {
            binding.vDivider.visibility = if (position == mlist.size - 1) View.GONE else View.VISIBLE
            binding.rvPhotos.visibility = if (it.momentType == 2) View.VISIBLE else View.GONE
            if (it.momentType == 2) {//显示图片集合
                val colums = when (it.imageUrl.size) {
                    1 -> 1 //单图
                    4 -> 2 //两列
                    else -> 3 //三列
                }
                binding.rvPhotos.layoutManager = GridLayoutManager(context, colums)
                val adapter = MomentImageAdapter(context) { pos ->
                    bigImageDialog = BigImageDialog(context, it.imageUrl, pos)
                    bigImageDialog?.show()
                }

                binding.rvPhotos.adapter = adapter
                adapter.initData(it.imageUrl)

                holder.itemView.setOnClickListener { _ ->
                    onItemClick(it)
                }
            }
        }
    }
}

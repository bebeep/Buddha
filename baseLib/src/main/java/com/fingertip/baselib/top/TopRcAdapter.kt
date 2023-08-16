package com.fingertip.baselib.top

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

@Suppress("UNCHECKED_CAST")
abstract class TopRcAdapter<T, VH: TopRcAdapter.TopRcViewHolder>(
   val context: Context
): RecyclerView.Adapter<VH>() {

    val TAG by lazy { javaClass.simpleName }

    val mlist = ArrayList<T>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = inflate(parent, initLayoutId(viewType))
        return TopRcViewHolder(v) as VH
    }

    override fun getItemCount(): Int = mlist.size

    fun inflate(parent: ViewGroup, resId: Int): View = LayoutInflater.from(context).inflate(resId, parent, false)

    abstract fun initLayoutId(viewType: Int): Int

    fun data() = mlist

    open fun initData(list: List<T>?) {
        list?.let {
            mlist.clear()
            mlist.addAll(list)
            notifyDataSetChanged()
        }
    }


    open fun addData(list: List<T>?) {
        list?.let {
            mlist.addAll(list)
            notifyDataSetChanged()
        }
    }

    fun get(position: Int) = if (position >= 0 && position < mlist.size) mlist[position] else null

    open class TopRcViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.BlessRecordBean
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.BlessRecordAdapter
import com.fingertip.uilib.databinding.DialogBlessListBinding
import com.fingertip.uilib.viewmodel.WishVM

/**
 * 愿望收到的祝福列表底部弹窗
 * 数据由外部（页面 VM）拉：首次传 append=false，翻页通过 onLoadMore(pageNum) 回调给外部
 */
class BlessListDialog(context: Context) : TopDialogBottomSheetDialog(context) {

    //需要下一页时回调，pageNum 从 1 开始（0 页由打开弹窗时外部拉）
    var onLoadMore: ((pageNum: Int) -> Unit)? = null

    private lateinit var binding: DialogBlessListBinding
    private lateinit var adapter: BlessRecordAdapter

    private var pageNum = 0
    private var hasMore = false
    private var loading = false

    override fun getLayoutId(): Int = R.layout.dialog_bless_list

    override fun onViewCreate(view: View) {
        binding = DialogBlessListBinding.bind(view)
        adapter = BlessRecordAdapter(context)
        binding.rcBlessList.layoutManager = LinearLayoutManager(context)
        binding.rcBlessList.adapter = adapter

        //滚到底部自动翻页
        binding.rcBlessList.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0 || loading || !hasMore) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val last = lm.findLastVisibleItemPosition()
                if (last >= adapter.itemCount - 3) {
                    loading = true
                    onLoadMore?.invoke(pageNum + 1)
                }
            }
        })
    }

    /**
     * 渲染祝福列表。append=false 首页全量替换，true 追加下一页
     */
    fun render(list: List<BlessRecordBean>?, append: Boolean) {
        loading = false
        val data = list.orEmpty()
        //一页不满说明到底了
        hasMore = data.size >= WishVM.PAGE_SIZE

        binding.tvEmpty.visibility = if (adapter.itemCount == 0 && data.isEmpty()) View.VISIBLE else View.GONE
        binding.rcBlessList.visibility = if (adapter.itemCount == 0 && data.isEmpty()) View.GONE else View.VISIBLE

        if (append) {
            adapter.addData(data)
            pageNum++
        } else {
            adapter.initData(data)
            pageNum = 0
        }
    }
}

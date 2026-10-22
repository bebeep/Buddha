package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.BookChapterItem
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FojingCatalogAdapter
import com.fingertip.uilib.databinding.DialogFojingCatalogBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior


/**
 * 经书目录，选中章节回调对应下标
 */
class FojingCatalogDialog(
    context: Context,
    private val items: List<BookChapterItem>,
    private val currentIndex: Int,
    private val onSelect: (index: Int) -> Unit
) : TopDialogBottomSheetDialog(context) {

    private lateinit var binding: DialogFojingCatalogBinding

    override fun getLayoutId() = R.layout.dialog_fojing_catalog

    override fun onViewCreate(view: View) {
        binding = DialogFojingCatalogBinding.bind(view)
        binding.tvCatalogCount.text = "共${items.size}章"
        binding.tvCatalogEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

        val layoutManager = LinearLayoutManager(context)
        val catalogAdapter = FojingCatalogAdapter(items, currentIndex) { index ->
            onSelect(index)
            dismiss()
        }
        binding.rvCatalog.layoutManager = layoutManager
        binding.rvCatalog.adapter = catalogAdapter

        //打开时定位到当前章，省得用户从卷一开始翻
        binding.rvCatalog.post {
            val position = catalogAdapter.positionOfChapter(currentIndex)
            if (position >= 0) {
                layoutManager.scrollToPositionWithOffset(position, 0)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        //固定占屏幕七成。BottomSheet 默认是收起态，只能露出标题那一条，
        //看着就像列表没加载出来，所以这里连收起态一起关掉
        val metrics = context.resources.displayMetrics
        val targetHeight = minOf(
            (metrics.heightPixels * 0.72f).toInt(),
            (560 * metrics.density).toInt()
        )
        behavior.skipCollapsed = true
        behavior.peekHeight = targetHeight
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { it.layoutParams.height = targetHeight }
    }
}

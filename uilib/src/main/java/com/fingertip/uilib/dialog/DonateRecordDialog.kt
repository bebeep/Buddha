package com.fingertip.uilib.dialog

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.top.TopDialogBottomSheetDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.DonateRecordAdapter
import com.fingertip.uilib.databinding.DialogDonateRecordBinding
import com.fingertip.uilib.viewmodel.DonateVM

/**
 * 功德捐赠记录弹窗
 */
class DonateRecordDialog(context: Context, private val viewModel: DonateVM) :
    TopDialogBottomSheetDialog(context) {

    private lateinit var binding: DialogDonateRecordBinding
    private lateinit var adapter: DonateRecordAdapter

    override fun getLayoutId(): Int = R.layout.dialog_donate_record

    override fun onViewCreate(view: View) {
        binding = DialogDonateRecordBinding.bind(view)
        adapter = DonateRecordAdapter(context)
        binding.rcRecords.layoutManager = LinearLayoutManager(context)
        binding.rcRecords.adapter = adapter
        binding.tvClose.setOnClickListener { dismiss() }

        viewModel.recordResult.observeForever { result ->
            if (result.success) {
                adapter.initData(result.data.orEmpty())
                binding.tvEmpty.visibility =
                    if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            }
        }
        viewModel.loadRecords(0)
    }
}

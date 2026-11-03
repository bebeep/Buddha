package com.fingertip.uilib.fragment.setting

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.UserBlockData
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.BlockUserAdapter
import com.fingertip.uilib.databinding.FragmentBlockListBinding
import com.fingertip.uilib.dialog.ConfirmDialog
import com.fingertip.uilib.viewmodel.UserVM

/**
 * 已拉黑用户
 *
 * 「拉黑」必须是可逆的，否则用户一旦误操作就只能靠申诉。
 * 这里列出全部拉黑记录并支持逐条解除，解除后对方的内容会重新出现在佛友圈。
 */
class BlockListFragment : TopPmFragment<UserVM>() {

    override fun layoutId() = R.layout.fragment_block_list

    override fun initVM(): UserVM = provideVM()

    private val binding get() = mBinding as FragmentBlockListBinding

    private lateinit var adapter: BlockUserAdapter

    //正在解除的对象，回调成功后从列表里摘掉
    private var pendingUnblock: UserBlockData? = null

    override fun initShiTu() {
        adapter = BlockUserAdapter(requireContext()) { item -> confirmUnblock(item) }
        binding.rvBlock.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBlock.adapter = adapter

        mViewModel.loadBlockList()
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.blockListResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            if (result.success) {
                val list = result.data ?: emptyList()
                adapter.initData(list)
                refreshEmptyState(list.isEmpty())
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.block_fail))
            }
        }

        mViewModel.unblockResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val target = pendingUnblock
            pendingUnblock = null
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.unblock_done))
                if (target != null) {
                    val index = adapter.mlist.indexOfFirst { it.targetAccountId == target.targetAccountId }
                    if (index != -1) {
                        adapter.mlist.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                }
                refreshEmptyState(adapter.mlist.isEmpty())
            } else {
                ToastUtil.showMessage(getString(R.string.unblock_fail))
            }
        }
    }

    private fun confirmUnblock(item: UserBlockData) {
        val name = item.targetUserName?.takeIf { it.isNotBlank() }
            ?: getString(R.string.block_list_name_unknown, item.targetAccountId)
        ConfirmDialog(
            requireContext(),
            title = getString(R.string.block_list_confirm_title),
            message = getString(R.string.block_list_confirm_msg_named, name),
            confirmText = getString(R.string.block_list_confirm_ok)
        ) {
            pendingUnblock = item
            mViewModel.cancelBlock(item.targetAccountId)
        }.show()
    }

    private fun refreshEmptyState(empty: Boolean) {
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvBlock.visibility = if (empty) View.GONE else View.VISIBLE
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
        }
    }
}

package com.fingertip.uilib.fragment.moment

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MomentAdapter
import com.fingertip.uilib.databinding.FragmentMyMomentBinding
import com.fingertip.uilib.dialog.ActionSheetDialog
import com.fingertip.uilib.dialog.ConfirmDialog
import com.fingertip.uilib.fragment.MainFragment
import com.fingertip.uilib.viewmodel.MomentVM
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener

/**
 * 我的感悟
 *
 * 自己发过的动态列表，长按删除。
 * 之所以必须做这一页：UGC 产品要能让用户删掉自己发布的内容（Apple 1.2 / Google UGC 政策），
 * 之前只能发不能删，提审是硬伤。
 *
 * 与佛友圈列表的区别：
 * - 只有自己，不走推荐/关注逻辑
 * - 长按给删除入口
 * - 不做视频自动播放（这一页是"管理我的内容"，不是刷内容）
 */
class MyMomentFragment : TopPmFragment<MomentVM>() {

    override fun layoutId() = R.layout.fragment_my_moment

    override fun initVM() = MomentVM()

    private val binding get() = mBinding as FragmentMyMomentBinding

    private var adapter: MomentAdapter? = null
    private val list = ArrayList<MomentEntity>()
    private var pageCount = 0
    private var loading = false

    //正在删除的那条，回调成功后从列表摘掉
    private var pendingDeleteId = 0

    override fun initShiTu() {
        adapter = MomentAdapter(
            requireContext(),
            onItemClick = { entity ->
                (parentFragment as? MainFragment)
                    ?.start(MomentDetailsFragment.newInstance(entity.momentId, entity))
            },
            onVideoClick = { entity ->
                (parentFragment as? MainFragment)
                    ?.start(MomentDetailsFragment.newInstance(entity.momentId, entity))
            },
            onItemLongClick = { entity -> showItemMenu(entity) }
        )
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerview.adapter = adapter
        adapter?.initData(list)

        binding.srl.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                pageCount = 0
                //上一轮如果已经"没有更多"，刷新后要把这个状态清掉，否则再也拉不到第二页
                binding.srl.resetNoMoreData()
                loadPage()
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (loading) return
                pageCount++
                loadPage()
            }
        })

        loadPage()
    }

    private fun loadPage() {
        loading = true
        mViewModel.getMomentListByAccount(0, pageCount)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.momentByAccountResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            loading = false
            if (result.success) {
                val data = result.data ?: emptyList()
                if (pageCount == 0) {
                    list.clear()
                    list.addAll(data)
                    adapter?.initData(list)
                } else {
                    list.addAll(data)
                    adapter?.addData(data)
                }
                if (data.isEmpty()) binding.srl.finishLoadMoreWithNoMoreData()
                else binding.srl.finishLoadMore()
                binding.srl.finishRefresh()
                refreshEmpty()
            } else {
                binding.srl.finishRefresh(false)
                binding.srl.finishLoadMore(false)
                if (pageCount > 0) pageCount--
            }
        }

        mViewModel.deleteMomentResult.observe(viewLifecycleOwner) { result ->
            if (!isAdded) return@observe
            val id = pendingDeleteId
            pendingDeleteId = 0
            if (result.success) {
                ToastUtil.showMessage(getString(R.string.my_moment_delete_done))
                val index = list.indexOfFirst { it.momentId == id }
                if (index != -1) {
                    list.removeAt(index)
                    adapter?.mlist?.removeAt(index)
                    adapter?.notifyItemRemoved(index)
                }
                refreshEmpty()
            } else {
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.my_moment_delete_fail))
            }
        }
    }

    private fun showItemMenu(entity: MomentEntity) {
        ActionSheetDialog(requireContext(), getString(R.string.my_moment_tip), listOf(
            ActionSheetDialog.Item(getString(R.string.my_moment_delete), danger = true) {
                ConfirmDialog(
                    requireContext(),
                    title = getString(R.string.my_moment_delete_title),
                    message = getString(R.string.my_moment_delete_msg),
                    confirmText = getString(R.string.my_moment_delete_ok)
                ) {
                    pendingDeleteId = entity.momentId
                    mViewModel.deleteMoment(entity.momentId)
                }.show()
            }
        )).show()
    }

    private fun refreshEmpty() {
        val empty = list.isEmpty()
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.srl.visibility = if (empty) View.GONE else View.VISIBLE
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
        }
    }
}

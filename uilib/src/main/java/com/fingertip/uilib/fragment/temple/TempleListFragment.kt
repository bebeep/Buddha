package com.fingertip.uilib.fragment.temple

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.TempleListAdapter
import com.fingertip.uilib.databinding.FragmentTempleListBinding
import com.fingertip.uilib.viewmodel.TempleVM

/**
 * 寺庙列表，对应原型「寺庙列表」与 temple 一组接口
 */
class TempleListFragment : TopPmFragment<TempleVM>() {

    companion object {
        private const val TAG = "TempleListFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_temple_list

    override fun initVM(): TempleVM = provideVM()

    private val binding get() = mBinding as FragmentTempleListBinding

    private lateinit var adapter: TempleListAdapter
    private var currPage = 0

    override fun initShiTu() {
        adapter = TempleListAdapter(requireContext()) { temple ->
            (parentFragment as? com.fingertip.uilib.fragment.MainFragment)?.start(
                TempleDetailFragment.newInstance(temple.id)
            )
        }
        binding.rcList.layoutManager = LinearLayoutManager(requireContext())
        binding.rcList.adapter = adapter

        binding.rcList.addOnScrollListener(object :
            androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && lm.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    currPage += 1
                    mViewModel.loadList(currPage)
                }
            }
        })

        mViewModel.loadList(0)
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.listResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                val list = result.data.orEmpty()
                if (currPage == 0) adapter.initData(list) else adapter.addData(list)
                binding.tvEmpty.visibility =
                    if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            } else {
                log(TAG, "寺庙列表获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败，请稍后再试")
            }
        }
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
        }
    }
}

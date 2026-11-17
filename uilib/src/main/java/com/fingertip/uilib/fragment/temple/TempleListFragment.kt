package com.fingertip.uilib.fragment.temple

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.TempleListAdapter
import com.fingertip.uilib.databinding.FragmentTempleListBinding
import com.fingertip.uilib.utils.openPage
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
    //防重入：上一次分页请求没回来之前不触发下一次，避免快速滚动连续拉页
    private var loadingList = false
    private var hasMore = true
    //按 id 去重：后端排序键相同行的翻页顺序可能不稳定，去重兜底
    private val loadedIds = mutableSetOf<Int>()

    override fun initShiTu() {
        adapter = TempleListAdapter(requireContext()) { temple ->
            openPage(TempleDetailFragment.newInstance(temple.id))
        }
        adapter.emptyText = "暂无古刹数据"
        binding.rcList.layoutManager = LinearLayoutManager(requireContext())
        binding.rcList.adapter = adapter

        binding.rcList.addOnScrollListener(object :
            androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && hasMore && !loadingList
                    && lm.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    loadingList = true
                    currPage += 1
                    mViewModel.loadList(currPage)
                }
            }
        })

        //首页列表等入栈动画放完再拉（带 loading HUD），转场不被抢帧；滚动加载不受影响
        postOnEnterAnimationEnd { mViewModel.loadList(0) }
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.listResult.observe(viewLifecycleOwner) { result ->
            if (result.success) {
                val list = result.data.orEmpty()
                if (currPage == 0) loadedIds.clear()
                val fresh = list.filter { loadedIds.add(it.id) }
                if (currPage == 0) adapter.initData(fresh) else adapter.addData(fresh)
                hasMore = list.isNotEmpty()
            } else {
                //失败回退页码，否则下次会跳页
                if (currPage > 0) currPage -= 1
                log(TAG, "寺庙列表获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败，请稍后再试")
            }
            loadingList = false
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

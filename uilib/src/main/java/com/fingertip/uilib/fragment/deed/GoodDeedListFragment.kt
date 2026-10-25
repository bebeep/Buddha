package com.fingertip.uilib.fragment.deed

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.fingertip.baselib.bean.DEED_CATEGORIES
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.DeedListAdapter
import com.fingertip.uilib.databinding.FragmentDeedListBinding
import com.fingertip.uilib.viewmodel.GoodDeedVM

/**
 * 行善主页，对应原型「行善-列表」：
 * 分类横滑筛选（全部+8类）、善行列表（官方优先）、右下角发布、我的行善入口
 */
class GoodDeedListFragment : TopPmFragment<GoodDeedVM>() {

    companion object {
        private const val TAG = "GoodDeedListFragment"
    }

    override fun layoutId(): Int = R.layout.fragment_deed_list

    override fun initVM(): GoodDeedVM = provideVM()

    private val binding get() = mBinding as FragmentDeedListBinding

    private lateinit var adapter: DeedListAdapter
    private var category: String? = ""
    private var currPage = 0

    //分类标签视图，便于切换选中态
    private val categoryViews = mutableListOf<Pair<String?, TextView>>()

    override fun initShiTu() {
        adapter = DeedListAdapter(requireContext()) { deed ->
            (parentFragment as? com.fingertip.uilib.fragment.MainFragment)?.start(
                GoodDeedDetailFragment.newInstance(deed.id)
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
                    mViewModel.loadList(currPage, category)
                }
            }
        })

        renderCategories()
        mViewModel.loadList(0, category)
    }

    private fun renderCategories() {
        val container = binding.llCategory
        container.removeAllViews()
        categoryViews.clear()
        val all = listOf<Pair<String?, String>>(null to "全部") +
                DEED_CATEGORIES.map { it to it }
        all.forEachIndexed { index, (name, label) ->
            val tv = TextView(requireContext())
            tv.text = label
            tv.textSize = 13f
            tv.setPadding(28, 10, 28, 10)
            tv.gravity = Gravity.CENTER
            tv.setOnClickListener {
                category = name
                currPage = 0
                renderCategoryState()
                mViewModel.loadList(0, category)
            }
            val dp = (resources.displayMetrics.density * 6).toInt()
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp
            container.addView(tv, lp)
            categoryViews.add(name to tv)
        }
        renderCategoryState()
    }

    private fun renderCategoryState() {
        categoryViews.forEach { (name, tv) ->
            val selected = name == category
            tv.setBackgroundResource(
                if (selected) R.drawable.bg_praying_cover_selected else R.drawable.bg_praying_cover_normal
            )
            tv.setTextColor(if (selected) Color.parseColor("#C87A2B") else Color.parseColor("#8A6B3A"))
        }
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
                log(TAG, "善行列表获取失败：${result.errorCode} ${result.errorMsg}")
                ToastUtil.showMessage(result.errorMsg ?: "加载失败，请稍后再试")
            }
        }
    }

    //列表页滚动加载时需要更新 currPage，这里拦一下：改为在 observer 里同步
    override fun getClickViews(): List<View> = listOf(
        binding.ivBack, binding.tvMine, binding.tvPostDeed
    )

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
            binding.tvMine ->
                (parentFragment as? com.fingertip.uilib.fragment.MainFragment)?.start(GoodDeedMineFragment())
            binding.tvPostDeed ->
                (parentFragment as? com.fingertip.uilib.fragment.MainFragment)?.start(GoodDeedPostFragment())
        }
    }
}

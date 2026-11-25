package com.fingertip.uilib.fragment.search

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.KeyboardUtils
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.MomentAdapter
import com.fingertip.uilib.adapter.SearchUserAdapter
import com.fingertip.uilib.databinding.FragmentSearchBinding
import com.fingertip.uilib.databinding.ItemSearchHistoryBinding
import com.fingertip.uilib.fragment.moment.MomentDetailsFragment
import com.fingertip.uilib.fragment.user.UserInfoFragment
import com.fingertip.uilib.utils.openPage
import com.fingertip.uilib.viewmodel.SearchVM
import org.json.JSONArray

/**
 * 佛友圈搜索：输入关键词同时搜佛友与动态。
 * 佛友展示基本信息卡片，动态复用列表卡片，命中的文本染成亮金。
 */
class SearchFragment : TopPmFragment<SearchVM>() {

    override fun initVM(): SearchVM = SearchVM()
    override fun layoutId() = R.layout.fragment_search

    private val binding get() = mBinding as FragmentSearchBinding

    private lateinit var searchUserAdapter: SearchUserAdapter
    private lateinit var searchMomentAdapter: MomentAdapter

    private var currentKeyword = ""

    private val historySp by lazy {
        requireContext().getSharedPreferences("search", 0)
    }

    override fun getClickViews(): List<View> = listOf(
        binding.ivBack, binding.ivSearchGo, binding.ivSearchClear
    )

    override fun initShiTu() {
        initAdapter()
        renderHistory()

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(binding.etSearch.text?.toString().orEmpty())
                true
            } else false
        }

        binding.srlSearchResult.setOnRefreshListener { doSearch(currentKeyword) }
    }

    private fun goBack() = pop()

    private fun initAdapter() {
        searchUserAdapter = SearchUserAdapter(requireContext()) { user ->
            openPage(UserInfoFragment.newInstance(user.accountId))
        }
        binding.rvSearchUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchUserAdapter
        }

        searchMomentAdapter = MomentAdapter(
            requireContext(),
            onItemClick = { openPage(MomentDetailsFragment.newInstance(it.momentId, it)) },
            onUserClick = { openPage(UserInfoFragment.newInstance(it.postAccountId)) }
        )
        binding.rvSearchMoments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchMomentAdapter
        }
    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> goBack()
            binding.ivSearchGo -> doSearch(binding.etSearch.text?.toString().orEmpty())
            binding.ivSearchClear -> resetToHistory()
        }
    }

    override fun initObserver() {
        super.initObserver()
        mViewModel.searchResult.observe(viewLifecycleOwner) { result ->
            binding.srlSearchResult.isRefreshing = false
            if (!isAdded) return@observe
            if (result.success) {
                val users = result.data?.users.orEmpty()
                val moments = result.data?.moments.orEmpty()
                searchUserAdapter.initData(users)
                searchMomentAdapter.initData(moments)
                binding.tvGroupUser.text = getString(R.string.search_user_group, users.size)
                binding.tvGroupMoment.text = getString(R.string.search_moment_group, moments.size)
                showResult(users.isNotEmpty() || moments.isNotEmpty())
            } else {
                searchUserAdapter.initData(emptyList())
                searchMomentAdapter.initData(emptyList())
                showResult(false)
                ToastUtil.showMessage(result.errorMsg ?: getString(R.string.search_fail))
            }
        }
    }

    private fun showResult(hasData: Boolean) {
        binding.gpSearchHistory.visibility = View.GONE
        binding.srlSearchResult.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.tvSearchEmpty.visibility = if (hasData) View.GONE else View.VISIBLE
    }

    private fun resetToHistory() {
        binding.etSearch.setText("")
        currentKeyword = ""
        binding.srlSearchResult.isRefreshing = false
        binding.srlSearchResult.visibility = View.GONE
        binding.tvSearchEmpty.visibility = View.GONE
        binding.gpSearchHistory.visibility = View.VISIBLE
        renderHistory()
    }

    private fun doSearch(raw: String) {
        val keyword = raw.trim()
        if (keyword.isEmpty()) {
            ToastUtil.showMessage(getString(R.string.search_input_hint))
            return
        }
        currentKeyword = keyword
        saveHistory(keyword)
        searchUserAdapter.keyword = keyword
        searchMomentAdapter.highlightKeyword = keyword
        showResult(true)
        binding.srlSearchResult.isRefreshing = true
        //搜索触发后收键盘，结果不用被键盘挡住
        KeyboardUtils.hideSoftInput(requireActivity())
        mViewModel.search(keyword)
    }

    // ==================== 历史搜索（本机保存，最多 8 条） ====================

    private fun renderHistory() {
        binding.rvSearchHistory.removeAllViews()
        val history = loadHistory()
        if (history.isEmpty()) {
            //还没有历史就只留搜索栏，不摆一个"没找到"的空态误导用户
            binding.gpSearchHistory.visibility = View.GONE
            return
        }
        binding.gpSearchHistory.visibility = View.VISIBLE
        history.forEach { keyword ->
            val item = ItemSearchHistoryBinding.inflate(layoutInflater, binding.rvSearchHistory, false)
            item.tvSearchHistoryItem.text = keyword
            item.root.setOnClickListener {
                binding.etSearch.setText(keyword)
                doSearch(keyword)
            }
            binding.rvSearchHistory.addView(item.root)
        }
    }

    private fun loadHistory(): List<String> {
        val raw = historySp.getString("keywords", "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(keyword: String) {
        val list = loadHistory().toMutableList()
        list.remove(keyword)
        list.add(0, keyword)
        val array = JSONArray()
        list.take(8).forEach { array.put(it) }
        historySp.edit().putString("keywords", array.toString()).apply()
    }
}

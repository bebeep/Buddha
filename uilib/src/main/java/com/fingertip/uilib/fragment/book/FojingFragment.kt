package com.fingertip.uilib.fragment.book

import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.BookshelfVM
import com.fingertip.baselib.top.TopFragmentPagerAdapter
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.uilib.databinding.FragFojingBinding
import com.weikaiyun.fragmentation.SupportFragment

/**
 * 佛经
 */
class FojingFragment :TopVMFragment<BookshelfVM>(){

    override fun layoutId() = R.layout.frag_fojing
    override fun initVM() = BookshelfVM()

    private val binding get() = mBinding as FragFojingBinding


    override fun initShiTu() {
        mViewModel.getBookTypeList()
    }

    override fun initObserver() {
        super.initObserver()
        mViewModel.bookTypeResult.observe(this){
            if (!it.success) return@observe
            val data = it.data ?: return@observe
            // View 已销毁时 binding 为 null，直接忽略，避免 as 强转崩溃
            if (mBinding == null) return@observe
            // 已经初始化过一次就不再重建 adapter：重复 setAdapter 会让 ViewPager 把已添加的
            // 页面移除，和 Fragmentation 的异步事务互相踩踏（也是本次崩溃的诱因之一）
            if (binding.vp.adapter != null) return@observe

            // 标题与页面一次遍历同时生成，保证 tab 与页面严格一一对应
            val fragments: ArrayList<SupportFragment> = ArrayList()
            val titles = ArrayList<String>()
            data.forEach { a ->
                when {
                    a.bookType == 1 -> {
                        fragments.add(FojingSubjectFragment())
                        titles.add(a.title ?: "")
                    }
                    a.bookType > 0 -> {
                        fragments.add(FojingChildFragment.newInstance(a.bookType))
                        titles.add(a.title ?: "")
                    }
                    // bookType 非法（后端没返回该字段时为默认值 0）：不生成 tab，避免出现空白页
                }
            }
            if (fragments.isEmpty()) return@observe

            // 取代原来的 postDelayed(600)：本页是 BuddhaTextsFragment 里 ViewPager 的一页，自身没有动画，
            // 但宿主页是带入栈动画打开的；而这里会一次性 add 全部子页面（每个都带 RecyclerView），
            // 压在动画期间必然掉帧。postOnEnterAnimationEnd 会向上检查宿主页的动画，
            // 真正播完才建 pager；数据回得比动画晚则立即执行，不用白等固定的 600ms。
            postOnEnterAnimationEnd { setupPager(fragments, titles) }

        }
    }

    /** 建 ViewPager（只在首次执行：重复 setAdapter 会让 pager 移除已添加的页面，与 Fragmentation 的异步事务互相踩踏） */
    private fun setupPager(fragments: ArrayList<SupportFragment>, titles: List<String>) {
        // View 已销毁时 post 会排进 View 的 runQueue，等下次 attach 才执行，不会崩
        (mBinding as? FragFojingBinding)?.vp?.post {
            val b = mBinding as? FragFojingBinding ?: return@post
            if (b.vp.adapter != null) return@post
            b.vp.offscreenPageLimit = fragments.size
            b.vp.adapter = TopFragmentPagerAdapter(fragments, childFragmentManager)
            b.tabLayout.setViewPager(b.vp, titles.toTypedArray())
        }
    }


}
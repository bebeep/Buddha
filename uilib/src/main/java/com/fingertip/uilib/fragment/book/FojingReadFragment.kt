package com.fingertip.uilib.fragment.book

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.viewpager2.widget.ViewPager2
import com.fingertip.baselib.bean.BookChapter
import com.fingertip.baselib.bean.BookChapterItem
import com.fingertip.baselib.bean.BookReadSetting
import com.fingertip.baselib.download.ChapterDownloader
import com.fingertip.baselib.reading.ReadingDict
import com.fingertip.baselib.reading.ReadingEntry
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.FojingReadPagerAdapter
import com.fingertip.uilib.databinding.FragFojingReadBinding
import com.fingertip.uilib.dialog.FojingCatalogDialog
import com.fingertip.uilib.dialog.FojingReadSettingDialog
import com.fingertip.uilib.dialog.FojingReadingDialog
import com.fingertip.uilib.viewmodel.ReadVM


/**
 * 佛经-读经
 * 一页一章，左右翻章，页内上下滚动
 */
class FojingReadFragment : TopVMFragment<ReadVM>() {

    companion object {
        private const val BOOK_ID = "BOOK_ID"
        private const val BOOK_TITLE = "BOOK_TITLE"

        fun newInstance(bookId: Int, bookTitle: String): FojingReadFragment {
            return FojingReadFragment().apply {
                arguments = Bundle().apply {
                    putInt(BOOK_ID, bookId)
                    putString(BOOK_TITLE, bookTitle)
                }
            }
        }
    }

    private val binding get() = mBinding as FragFojingReadBinding
    override fun layoutId() = R.layout.frag_fojing_read
    override fun initVM() = ReadVM()

    private var bookId = 0
    private var bookTitle = ""
    private var adapter: FojingReadPagerAdapter? = null
    private var chapters: List<BookChapterItem> = emptyList()

    //显示设置，服务端回来前先用默认
    private var setting = BookReadSetting()

    //进度里带回来的起始章和字符位置，等目录/正文到齐后再定位
    private var pendingSeq = 0
    private var pendingOffset = 0

    //只在首次进入时恢复一次位置，之后翻页不受进度回调影响
    private var restored = false

    //等正文渲染完要滚过去的目标位置，滚完即清
    private var restoreSeq = 0
    private var restoreOffset = 0

    //当前阅读位置，翻页和滚动停止时更新，退出时用它上报
    private var curSeq = 0
    private var curOffset = 0

    //正在请求的章节，防止同一章被反复请求
    private val loading = HashSet<Int>()

    //设置弹窗，下载进度要往里推
    private var settingDialog: FojingReadSettingDialog? = null

    //下载结束的提示只弹一次
    private var downloadNoticeShown = false

    //本页期间看到过下载中，才在结束时提示
    private var sawDownloading = false

    override fun initShiTu() {
        bookId = arguments?.getInt(BOOK_ID, 0) ?: 0
        bookTitle = arguments?.getString(BOOK_TITLE).orEmpty()
        if (bookId <= 0) {
            pop()
            return
        }
        binding.tvTitle.text = bookTitle
        binding.vpContent.offscreenPageLimit = 1
        binding.vpContent.registerOnPageChangeCallback(pageCallback)
        //读音库装在内存里，点字查读音要用
        ReadingDict.load(requireContext())
        applySetting()

        mViewModel.loadCatalog(bookId)
        mViewModel.loadProgress(bookId)
        mViewModel.loadReadSetting()
    }

    override fun getClickViews(): List<View> {
        return listOf(binding.vBack.ivBack, binding.tvCatalog, binding.ivSetting)
    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v?.id){
            R.id.v_back -> pop()
            R.id.tv_catalog -> showCatalog()
            R.id.iv_setting -> showSetting()
        }
    }

    /**
     * 显示设置：背景、顶栏配色、正文的字号行距，以及阅读时常亮
     */
    private fun applySetting() {
        val night = setting.isNight()
        binding.root.setBackgroundColor(setting.bgColor())
        binding.tvTitle.setTextColor(setting.titleColor())
        binding.tvPage.setTextColor(if (night) 0x80FFFEFA.toInt() else 0xFFB3A48E.toInt())
        binding.tvCatalog.setTextColor(if (night) 0xFFFFD08A.toInt() else 0xFF8C6B3F.toInt())
        //深色底上图标要反白，不然看不见
        if (night) {
            binding.vBack.ivBack.setColorFilter(Color.WHITE)
            binding.ivSetting.setColorFilter(Color.WHITE)
        } else {
            binding.vBack.ivBack.clearColorFilter()
            binding.ivSetting.clearColorFilter()
        }
        adapter?.applySetting(setting)
        //字号行距一变，原来的滚动像素就不对应原来的句子了，按字符位置重新对齐
        if (curSeq > 0) adapter?.scrollToOffset(curSeq, curOffset)
        applyKeepScreenOn(setting.keepOn())
    }

    private fun applyKeepScreenOn(on: Boolean) {
        val window = activity?.window ?: return
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun showSetting() {
        val dialog = FojingReadSettingDialog(
            requireContext(),
            setting,
            downloadState = ChapterDownloader.progress.value?.takeIf { it.bookId == bookId },
            onDownload = { toggleDownload() },
            onPreview = { applySetting() },
            onSave = { mViewModel.saveReadSetting(it) }
        )
        settingDialog = dialog
        dialog.show()
    }

    /**
     * 整部离线：没下载就开，下载中就停
     */
    private fun toggleDownload() {
        if (chapters.isEmpty()) {
            ToastUtil.showMessage("目录还在加载")
            return
        }
        if (ChapterDownloader.isDownloading(bookId)) {
            ChapterDownloader.cancel()
            ToastUtil.showMessage("已停止，下好的章节保留")
        } else {
            ChapterDownloader.start(bookId, chapters)
            ToastUtil.showMessage("开始下载，退出阅读页也会继续")
        }
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.catalogResult.observe(viewLifecycleOwner) {
            if (!isAdded) return@observe
            if (it.success) {
                val list = it.data ?: return@observe
                if (list.isEmpty()) {
                    ToastUtil.showMessage("这部经还没有正文")
                    return@observe
                }
                setupPager(list)
            } else {
                ToastUtil.showMessage("目录加载失败")
            }
        }

        mViewModel.chapterResult.observe(viewLifecycleOwner) {
            val chapter = it.data
            if (it.success && chapter != null) {
                loading.remove(chapter.seq)
                adapter?.submitChapter(chapter)
                if (chapter.seq == restoreSeq) {
                    //恢复目标章渲染完了，滚到上次读到的位置
                    restoreSeq = 0
                    curOffset = restoreOffset
                    adapter?.scrollToOffset(chapter.seq, restoreOffset)
                }
            } else {
                //失败时清空，下次翻回这章会重新请求
                loading.clear()
            }
        }

        mViewModel.progressResult.observe(viewLifecycleOwner) {
            if (restored) return@observe
            if (!it.success) return@observe
            val progress = it.data ?: return@observe
            if (progress.lastReadSeq <= 0) {
                restored = true
                return@observe
            }
            pendingOffset = progress.lastReadOffset
            if (chapters.isEmpty()) {
                //目录还没到，先记下起始章
                pendingSeq = progress.lastReadSeq
            } else {
                jumpToSeq(progress.lastReadSeq)
            }
        }

        mViewModel.settingResult.observe(viewLifecycleOwner) {
            if (!it.success) return@observe
            val data = it.data ?: return@observe
            setting = data
            applySetting()
        }

        ChapterDownloader.progress.observe(viewLifecycleOwner) {
            if (it.bookId != bookId) return@observe
            settingDialog?.takeIf { dialog -> dialog.isShowing }?.updateDownload(it)
            if (it.running) {
                //看到过下载中，结束才值得提示；进页面时本来就已经下的不弹
                sawDownloading = true
                return@observe
            }
            if (!sawDownloading || downloadNoticeShown) return@observe
            downloadNoticeShown = true
            val error = it.error
            if (error != null) {
                ToastUtil.showMessage(error)
            } else if (it.finished()) {
                ToastUtil.showMessage("整部经文已缓存，断网也能读")
            }
        }
    }

    /**
     * 目录到货后装配 ViewPager2
     */
    private fun setupPager(items: List<BookChapterItem>) {
        if (!isAdded) return
        chapters = items
        val current = adapter
        if (current == null) {
            val created = FojingReadPagerAdapter(items)
            created.onScrollStopped = { seq, offset ->
                curSeq = seq
                curOffset = offset
                mViewModel.storeReadProgress(bookId, seq, offset)
            }
            created.onWordClick = { entry -> showReading(entry) }
            created.applySetting(setting)
            adapter = created
            binding.vpContent.adapter = created
        } else {
            current.submitCatalog(items)
        }
        binding.tvPage.text = "1 / ${items.size}"
        //把这本书已缓存多少章摆正，下载中断过也能接着看还剩多少
        ChapterDownloader.refresh(bookId, items.size)

        val target = pendingSeq
        if (target > 0) {
            pendingSeq = 0
            restored = true
            restoreSeq = target
            restoreOffset = pendingOffset
            val index = adapter?.indexOfSeq(target) ?: -1
            if (index >= 0) {
                binding.vpContent.setCurrentItem(index, false)
            }
        }
        loadChapterAt(binding.vpContent.currentItem)
    }

    private fun jumpToSeq(seq: Int) {
        restored = true
        restoreSeq = seq
        restoreOffset = pendingOffset
        val index = adapter?.indexOfSeq(seq) ?: -1
        if (index < 0) return
        binding.vpContent.setCurrentItem(index, false)
    }

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (!isAdded) return
            val total = adapter?.itemCount ?: 0
            binding.tvPage.text = "${position + 1} / $total"
            curSeq = adapter?.getItem(position)?.seq ?: 0
            //恢复进度引起的选页不算真翻页，位置等正文渲染完按恢复点对齐
            curOffset = if (restoreSeq > 0) restoreOffset else 0
            //正文已经在内存里就不会再有回调，直接对齐，不然这次恢复就白等了
            if (restoreSeq > 0 && adapter?.getChapter(restoreSeq) != null) {
                val seq = restoreSeq
                val offset = restoreOffset
                restoreSeq = 0
                binding.vpContent.post { adapter?.scrollToOffset(seq, offset) }
            }
            loadChapterAt(position)
            //预取下一章，翻页不用等
            loadChapterAt(position + 1)
        }
    }

    /**
     * 该章还没进内存就去取（先 Room 后网络）
     */
    private fun loadChapterAt(position: Int) {
        val item = adapter?.getItem(position) ?: return
        if (adapter?.getChapter(item.seq) != null) return
        if (!loading.add(item.seq)) return
        mViewModel.loadChapter(bookId, item.seq)
    }

    private fun showCatalog() {
        if (chapters.isEmpty()) {
            ToastUtil.showMessage("目录还在加载")
            return
        }
        FojingCatalogDialog(requireContext(), chapters, binding.vpContent.currentItem) { index ->
            binding.vpContent.setCurrentItem(index, false)
        }.show()
    }

    /**
     * 点字查读音
     */
    private fun showReading(entry: ReadingEntry) {
        if (!isAdded) return
        FojingReadingDialog(requireContext(), entry).show()
    }

    override fun onPause() {
        //离开阅读页把位置落库并上报，切后台也一样
        if (curSeq > 0) {
            mViewModel.reportReadProgress(bookId, curSeq, curOffset)
        }
        //常亮只在阅读页里要，别把别的页面也带着亮
        applyKeepScreenOn(false)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        applyKeepScreenOn(setting.keepOn())
    }

    override fun onDestroyView() {
        binding.vpContent.unregisterOnPageChangeCallback(pageCallback)
        binding.vpContent.adapter = null
        adapter = null
        settingDialog = null
        super.onDestroyView()
    }
}

package com.fingertip.uilib.fragment.book

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.BookChapter
import com.fingertip.baselib.bean.BookChapterItem
import com.fingertip.baselib.bean.BookCopySetting
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopVMFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.CopyCell
import com.fingertip.uilib.adapter.FojingCopyAdapter
import com.fingertip.uilib.databinding.FragFojingCopyBinding
import com.fingertip.uilib.dialog.FojingCopySettingDialog
import com.fingertip.uilib.viewmodel.CopyVM
import com.lzlz.toplib.extention.onGlobalLayout
import com.lzlz.toplib.extention.toPx

/**
 * 佛经-抄经
 * 不写字：格子只收汉字，上半格淡色列原文，用户在底部四个选项里挑出当前字填进下半格。
 * 一屏抄满自动换屏，一章抄完自动进下一章；长按当前格可以跳过这个字
 */
class FojingCopyFragment : TopVMFragment<CopyVM>() {

    companion object {
        private const val BOOK_ID = "BOOK_ID"
        private const val BOOK_TITLE = "BOOK_TITLE"

        //抄满这么多字上报一次
        private const val REPORT_STEP = 20

        //本章字池不够时用这些常用字兜底，保证四个选项总是满的
        private val FALLBACK_CHARS = listOf(
            "佛", "菩", "萨", "观", "自", "在", "空", "色", "受", "想", "行", "识",
            "无", "明", "尽", "老", "死", "苦", "集", "灭", "道", "智", "得", "故",
            "般", "若", "波", "罗", "蜜", "多", "心", "经", "如", "是", "闻", "时"
        )

        fun newInstance(bookId: Int, bookTitle: String): FojingCopyFragment {
            return FojingCopyFragment().apply {
                arguments = Bundle().apply {
                    putInt(BOOK_ID, bookId)
                    putString(BOOK_TITLE, bookTitle)
                }
            }
        }
    }

    private val binding get() = mBinding as FragFojingCopyBinding
    override fun layoutId() = R.layout.frag_fojing_copy
    override fun initVM() = CopyVM()

    private var bookId = 0
    private var bookTitle = ""

    //目录，按 seq 升序
    private var chapters: List<BookChapterItem> = emptyList()

    //全书字数，算百分比用
    private var totalChars = 0

    //当前抄的章
    private var seq = 0
    private var chapterName = ""

    //已经构建成格子的章号，同章的网络回调不再重建，免得把抄到的位置冲掉
    private var loadedSeq = 0

    //整章要抄的字，只收汉字，标点和空白不进格
    private var cells: List<CopyCell> = emptyList()

    //本章出现过的字，用来生成干扰项
    private var charPool: List<String> = emptyList()

    //正文字符串长度，抄到章末时上报用
    private var contentLength = 0

    //下一个待抄的字在 cells 里的下标
    private var cursor = 0

    //游标所在屏的起点
    private var cursorScreenStart = 0

    //当前屏幕显示的屏起点，回看时和 cursorScreenStart 不一致
    private var viewStart = 0

    //一屏能放多少格
    private var screenCount = 0

    //目录和进度都到齐才能开始
    private var catalogReady = false
    private var progressReady = false
    private var started = false

    //服务端进度：从哪一章第几个字符开始
    private var startSeq = 0
    private var startOffset = 0

    //累计抄写字数（含重复抄写）
    private var copiedChars = 0L

    //本批待上报的抄写字数、跳过格数和起始时间
    private var batchChars = 0
    private var batchSkipped = 0
    private var batchStartAt = 0L

    //显示设置，进页面从服务端拉一次
    private var setting = BookCopySetting()

    private var adapter: FojingCopyAdapter? = null

    override fun initShiTu() {
        bookId = arguments?.getInt(BOOK_ID, 0) ?: 0
        bookTitle = arguments?.getString(BOOK_TITLE).orEmpty()
        if (bookId <= 0) {
            pop()
            return
        }
        binding.tvTitle.text = bookTitle
        binding.tvScreen.text = "正文加载中…"
        batchStartAt = System.currentTimeMillis()

        initAdapter()
        mViewModel.loadCatalog(bookId)
        mViewModel.loadProgress(bookId)
        mViewModel.loadCopySetting()
    }

    override fun getClickViews(): List<View> {
        return listOf(
            binding.vBack.ivBack, binding.ivSetting,
            binding.tvPrev, binding.tvNext,
            binding.tvAnswer1, binding.tvAnswer2, binding.tvAnswer3, binding.tvAnswer4
        )
    }

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v?.id) {
            R.id.v_back -> pop()
            R.id.iv_setting -> showSetting()
            R.id.tv_prev -> goPrevScreen()
            R.id.tv_next -> goNextScreen()
            R.id.tv_answer1 -> onAnswerClick(binding.tvAnswer1)
            R.id.tv_answer2 -> onAnswerClick(binding.tvAnswer2)
            R.id.tv_answer3 -> onAnswerClick(binding.tvAnswer3)
            R.id.tv_answer4 -> onAnswerClick(binding.tvAnswer4)
        }
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.catalogResult.observe(viewLifecycleOwner) {
            if (!it.success) {
                ToastUtil.showMessage("目录加载失败")
                return@observe
            }
            val list = it.data
            if (list.isNullOrEmpty()) {
                ToastUtil.showMessage("这部经还没有正文")
                return@observe
            }
            chapters = list
            totalChars = list.sumOf { item -> item.charCount }
            catalogReady = true
            tryStart()
        }

        mViewModel.progressResult.observe(viewLifecycleOwner) {
            if (it.success) {
                val progress = it.data
                startSeq = progress?.lastCopySeq ?: 0
                startOffset = progress?.lastCopyOffset ?: 0
                copiedChars = progress?.copiedChars ?: 0
            }
            progressReady = true
            tryStart()
        }

        mViewModel.chapterResult.observe(viewLifecycleOwner) {
            val chapter = it.data
            if (!it.success || chapter == null) {
                //进度记的章可能已经不存在了（正文更新过），退回第一章重来
                val first = chapters.firstOrNull()
                if (first != null && seq != first.seq) {
                    seq = first.seq
                    loadedSeq = 0
                    mViewModel.loadChapter(bookId, seq)
                } else {
                    ToastUtil.showMessage("正文加载失败")
                }
                return@observe
            }
            if (chapter.seq == loadedSeq) return@observe
            buildChapter(chapter)
        }

        mViewModel.settingResult.observe(viewLifecycleOwner) {
            if (!it.success) return@observe
            val data = it.data ?: return@observe
            setting = data
            applySetting()
        }
    }

    /**
     * 按屏幕算一屏能放多少格，格子尺寸沿用原来设计的 40dp × 80dp
     */
    private fun initAdapter() {
        binding.flContent.onGlobalLayout {
            if (adapter == null) setupGrid()
        }
    }

    private fun setupGrid() {
        if (!isAdded || view == null || adapter != null) return
        val flContent = binding.flContent
        if (flContent.height <= 0 || flContent.width <= 0) {
            //尺寸还没出来，下一帧再算
            flContent.postDelayed({ setupGrid() }, 50)
            return
        }
        val row = (flContent.height - 30.toPx()) / 85.toPx()
        val column = (flContent.width - 32.toPx()) / 40.toPx()
        if (row <= 0 || column <= 0) return
        log(value = "fl_content  $row   $column  ")
        screenCount = row * column

        val created = FojingCopyAdapter(requireContext(), column)
        //长按当前格跳过这个字
        created.onCellLongClick = {
            if (it === cells.getOrNull(cursor)) skipCurrent()
        }
        adapter = created
        binding.rvContent.layoutManager = GridLayoutManager(requireContext(), column)
        binding.rvContent.adapter = created
        applySetting()

        //布局算得晚时正文可能已经到了，补铺一屏
        if (cells.isNotEmpty()) showScreen(cursorScreenStart)
        else clearOptions()
    }

    /**
     * 显示设置：背景、标题色、格子里的字色字号
     */
    private fun applySetting() {
        val dark = setting.backgroundColor == 2
        binding.root.setBackgroundColor(setting.bgColor())
        binding.tvTitle.setTextColor(if (dark) 0xFFFFFEFA.toInt() else 0xFF2C1900.toInt())
        //深色底上图标要反白，不然看不见
        if (dark) {
            binding.vBack.ivBack.setColorFilter(Color.WHITE)
            binding.ivSetting.setColorFilter(Color.WHITE)
        } else {
            binding.vBack.ivBack.clearColorFilter()
            binding.ivSetting.clearColorFilter()
        }
        adapter?.let {
            it.defaultTextColor = setting.defaultTextColor()
            it.copyTextColor = setting.copyTextColor()
            it.copyTextSize = setting.copyTextSize()
            it.notifyDataSetChanged()
        }
    }

    private fun showSetting() {
        FojingCopySettingDialog(
            requireContext(),
            setting,
            onPreview = { applySetting() },
            onSave = { mViewModel.saveCopySetting(it) }
        ).show()
    }

    /**
     * 目录和进度都到齐才决定从哪开始
     */
    private fun tryStart() {
        if (started || !catalogReady || !progressReady) return
        started = true
        if (chapters.isEmpty()) return
        val target = if (startSeq > 0) startSeq else chapters.first().seq
        seq = target
        mViewModel.loadChapter(bookId, target)
    }

    /**
     * 正文到货：切成格子，按服务端偏移定位
     */
    private fun buildChapter(chapter: BookChapter) {
        seq = chapter.seq
        chapterName = chapters.firstOrNull { item -> item.seq == chapter.seq }?.displayTitle()
            ?: "第${chapter.seq}章"
        contentLength = chapter.content.length

        val built = ArrayList<CopyCell>()
        chapter.content.forEachIndexed { index, c ->
            if (Character.isIdeographic(c.code)) built.add(CopyCell(c.toString(), index))
        }
        if (built.isEmpty()) {
            ToastUtil.showMessage("本章没有可抄的字")
            moveToNextChapter()
            return
        }

        loadedSeq = chapter.seq
        cells = built
        charPool = built.map { item -> item.char }.distinct()

        //进度里存的是正文字符下标，换算成本章第几个汉字
        var start = hanIndexAt(chapter.content, startOffset)
        if (start >= built.size) start = 0
        startOffset = 0
        cursor = start
        cursorScreenStart = start
        viewStart = start
        showScreen(start)
    }

    /**
     * 铺一屏格子。start 是这屏第一个字在 cells 里的下标
     */
    private fun showScreen(start: Int) {
        if (cells.isEmpty() || start < 0 || start >= cells.size) return
        viewStart = start
        val end = minOf(start + screenCount, cells.size)
        val screen = ArrayList<CopyCell>()
        for (i in start until end) {
            val cell = cells[i]
            //已经抄过的字保留，没抄的留空；只有当前屏才标当前格
            cell.typed = if (i < cursor) cell.char else ""
            cell.current = start == cursorScreenStart && i == cursor
            screen.add(cell)
        }
        adapter?.initData(screen)
        refreshScreenInfo()
        if (start == cursorScreenStart) updateOptions() else clearOptions()
    }

    private fun goPrevScreen() {
        if (viewStart <= 0) {
            ToastUtil.showMessage("已经是第一屏")
            return
        }
        showScreen(maxOf(0, viewStart - screenCount))
    }

    private fun goNextScreen() {
        if (viewStart >= cursorScreenStart) {
            ToastUtil.showMessage("已经是当前抄写屏")
            return
        }
        showScreen(minOf(viewStart + screenCount, cursorScreenStart))
    }

    /**
     * 四个选项：一个对，三个从本章出现过的字里随机取，不够就用常用字补齐
     */
    private fun updateOptions() {
        if (cursor >= cells.size) {
            clearOptions()
            return
        }
        val answer = cells[cursor].char
        val pool = charPool.filter { it != answer }.shuffled().take(3).toMutableList()
        if (pool.size < 3) {
            for (c in FALLBACK_CHARS) {
                if (pool.size >= 3) break
                if (c != answer && !pool.contains(c)) pool.add(c)
            }
        }
        val options = (pool + answer).shuffled()
        val views = answerViews()
        views.forEachIndexed { index, tv ->
            tv.text = options.getOrNull(index).orEmpty()
        }
    }

    private fun clearOptions() {
        answerViews().forEach { it.text = "" }
    }

    private fun answerViews() = listOf(
        binding.tvAnswer1, binding.tvAnswer2, binding.tvAnswer3, binding.tvAnswer4
    )

    /**
     * 点选项：对了填格往前，错了原地重来
     */
    private fun onAnswerClick(tv: TextView) {
        val choice = tv.text.toString()
        if (choice.isEmpty() || cursor >= cells.size) return
        if (viewStart != cursorScreenStart) {
            ToastUtil.showMessage("回看中，先回当前屏再抄")
            return
        }
        val cell = cells[cursor]
        if (choice != cell.char) {
            //抖一下提示选错了，不打断当前进度
            shake(tv)
            return
        }
        val donePos = cursor - viewStart
        cell.typed = cell.char
        cell.current = false
        copiedChars++
        batchChars++
        advance(donePos)
    }

    /**
     * 长按当前格跳过这个字，跳过的不计功德但算前进
     */
    private fun skipCurrent() {
        if (cursor >= cells.size || viewStart != cursorScreenStart) return
        val cell = cells[cursor]
        val donePos = cursor - viewStart
        cell.typed = cell.char
        cell.skipped = true
        cell.current = false
        batchSkipped++
        ToastUtil.showMessage("已跳过「${cell.char}」")
        advance(donePos)
    }

    /**
     * 游标往前走一格，顺手处理换屏、换章
     */
    private fun advance(donePos: Int) {
        cursor++
        if (cursor >= cells.size) {
            onChapterFinished()
            return
        }
        if (cursor - cursorScreenStart >= screenCount) {
            //本屏抄满，换下一屏
            cursorScreenStart = cursor
            adapter?.notifyItemChanged(donePos)
            showScreen(cursor)
        } else {
            cells[cursor].current = true
            adapter?.notifyItemChanged(donePos)
            adapter?.notifyItemChanged(cursor - viewStart)
            refreshPercent()
            updateOptions()
        }
        if (batchChars + batchSkipped >= REPORT_STEP) flushProgress()
    }

    private fun onChapterFinished() {
        flushProgress()
        ToastUtil.showMessage("《$chapterName》已抄完")
        moveToNextChapter()
    }

    private fun moveToNextChapter() {
        val index = chapters.indexOfFirst { it.seq == seq }
        if (index < 0 || index + 1 >= chapters.size) {
            ToastUtil.showMessage("这部经已经抄完了")
            clearOptions()
            return
        }
        startOffset = 0
        cells = emptyList()
        charPool = emptyList()
        cursor = 0
        cursorScreenStart = 0
        viewStart = 0
        loadedSeq = 0
        val next = chapters[index + 1]
        binding.tvScreen.text = next.displayTitle()
        adapter?.initData(emptyList<CopyCell>())
        clearOptions()
        mViewModel.loadChapter(bookId, next.seq)
    }

    private fun refreshScreenInfo() {
        val totalScreen = if (cells.isEmpty() || screenCount <= 0) 0
        else (cells.size + screenCount - 1) / screenCount
        val screenNo = if (screenCount <= 0) 0 else viewStart / screenCount + 1
        val lookBack = if (viewStart != cursorScreenStart) " · 回看" else ""
        binding.tvScreen.text = "$chapterName  $screenNo/$totalScreen 屏$lookBack"
        binding.tvPrev.alpha = if (viewStart > 0) 1f else 0.3f
        binding.tvNext.alpha = if (viewStart < cursorScreenStart) 1f else 0.3f
        refreshPercent()
    }

    private fun refreshPercent() {
        binding.tvPercent.text = "${percentOfBook()}%"
    }

    /**
     * 全书进度：前面章的字数全算，当前章按已抄格数折算
     */
    private fun percentOfBook(): Int {
        if (totalChars <= 0 || chapters.isEmpty()) return 0
        var done = 0L
        for (item in chapters) {
            if (item.seq < seq) {
                done += item.charCount
            } else if (item.seq == seq && cells.isNotEmpty()) {
                done += item.charCount.toLong() * cursor / cells.size
                break
            }
        }
        return (done * 100L / totalChars).toInt().coerceIn(0, 100)
    }

    /**
     * 选错时左右抖一下
     */
    private fun shake(view: View) {
        view.animate().translationX(8f).setDuration(50).withEndAction {
            view.animate().translationX(-8f).setDuration(50).withEndAction {
                view.animate().translationX(0f).setDuration(50).start()
            }.start()
        }.start()
    }

    /**
     * 当前待抄字在正文里的下标
     */
    private fun currentOffset(): Int {
        if (cells.isEmpty()) return 0
        return if (cursor < cells.size) cells[cursor].sourceIndex else contentLength
    }

    /**
     * 上报：先落本地，再报服务端增量
     */
    private fun flushProgress() {
        if (!started) return
        //正文还没铺开（或正在换章），位置没有意义，别把已存的进度冲掉
        if (cells.isEmpty()) return
        val offset = currentOffset()
        mViewModel.saveLocalProgress(bookId, seq, offset, copiedChars)
        if (batchChars <= 0 && batchSkipped <= 0) return
        val duration = ((System.currentTimeMillis() - batchStartAt) / 1000).toInt().coerceIn(0, 1800)
        mViewModel.reportCopyProgress(bookId, seq, offset, batchChars, duration, batchSkipped)
        batchChars = 0
        batchSkipped = 0
        batchStartAt = System.currentTimeMillis()
    }

    /**
     * 正文字符下标换算成第几个汉字
     */
    private fun hanIndexAt(content: String, charOffset: Int): Int {
        if (charOffset <= 0) return 0
        var count = 0
        val end = minOf(charOffset, content.length)
        for (i in 0 until end) {
            if (Character.isIdeographic(content[i].code)) count++
        }
        return count
    }

    override fun onPause() {
        //离开抄经页把位置和字数交上去
        flushProgress()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.rvContent.adapter = null
        adapter = null
        super.onDestroyView()
    }
}

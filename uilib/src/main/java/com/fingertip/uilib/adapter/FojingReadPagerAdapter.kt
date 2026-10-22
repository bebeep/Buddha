package com.fingertip.uilib.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.BookChapter
import com.fingertip.baselib.bean.BookChapterItem
import com.fingertip.baselib.bean.BookReadSetting
import com.fingertip.baselib.reading.ReadingDict
import com.fingertip.baselib.reading.ReadingEntry
import com.fingertip.uilib.databinding.ItemFojingReadPageBinding
import kotlin.math.abs


/**
 * 读经翻章：一页 = 一章的可滚动正文
 * 目录来自服务端，正文按需从 Room/网络补进来，已加载的章节缓存在内存里
 */
class FojingReadPagerAdapter(items: List<BookChapterItem>) :
    RecyclerView.Adapter<FojingReadPagerAdapter.PageHolder>() {

    companion object {
        //滚动停下 300ms 才算阅读位置稳定，再采集
        private const val SCROLL_SETTLE_MS = 300L
    }

    private val items = ArrayList<BookChapterItem>(items)
    private val chapters = HashMap<Int, BookChapter>()

    //阅读显示设置，字号/行距/文字色/背景都从这里取
    var setting: BookReadSetting? = null
        private set

    /**
     * 应用显示设置。字号变了以后滚动位置会漂，调用方按字符位置再对齐一次
     */
    fun applySetting(value: BookReadSetting) {
        setting = value
        notifyDataSetChanged()
    }

    //滚动的位置稳定后回调，用来落阅读进度
    var onScrollStopped: ((seq: Int, offset: Int) -> Unit)? = null

    //点了正文里的字，命中读音库就给页面去弹窗
    var onWordClick: ((ReadingEntry) -> Unit)? = null

    private val settleHandler = Handler(Looper.getMainLooper())
    private var settleHolder: PageHolder? = null
    private val settleTask = Runnable {
        val holder = settleHolder ?: return@Runnable
        val offset = holder.visibleCharOffset()
        if (offset >= 0) {
            onScrollStopped?.invoke(holder.seq, offset)
        }
    }

    private var attachedRv: RecyclerView? = null

    class PageHolder(val binding: ItemFojingReadPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        var seq = 0

        //这一页已经绑过一次正文，重绑（改字号）时不要再把滚动位置清零
        var bound = false

        //按下时的位置，用来判断是点击还是滑动
        var downX = 0f
        var downY = 0f

        /**
         * 当前视口顶部对应正文的字符下标，正文还没布局完返回 -1
         */
        fun visibleCharOffset(): Int {
            val layout = binding.tvChapterContent.layout ?: return -1
            //标题和 padding 都体现在 content 的 top 里，直接用 scrollY 减掉
            val y = binding.root.scrollY - binding.tvChapterContent.top
            if (y <= 0) return 0
            val line = layout.getLineForVertical(y)
            return layout.getLineStart(line)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        attachedRv = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        attachedRv = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val binding = ItemFojingReadPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val holder = PageHolder(binding)
        val touchSlop = ViewConfiguration.get(parent.context).scaledTouchSlop
        binding.tvChapterContent.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    holder.downX = event.x
                    holder.downY = event.y
                    //DOWN 必须吃掉。TextView 自己不消费的话，ScrollView 认为没人处理，
                    //后面的 UP 就不会再派回来，点击永远判不出来。
                    //手指真滑动时 ScrollView 会自己拦截滚动并发 CANCEL，翻页不受影响
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                MotionEvent.ACTION_UP -> {
                    //滑完屏幕手抬起来不算点击，否则每滚一下都弹读音
                    val moved = abs(event.x - holder.downX) > touchSlop ||
                            abs(event.y - holder.downY) > touchSlop
                    if (!moved) {
                        findWordAt(view as TextView, event)?.let { onWordClick?.invoke(it) }
                    }
                    true
                }
                else -> false
            }
        }
        return holder
    }

    /**
     * 点击位置换成字符下标，再查读音库。
     * 点在字的边缘时下标可能落到旁边的标点上，左右各让一格再试
     */
    private fun findWordAt(textView: TextView, event: MotionEvent): ReadingEntry? {
        val textLayout = textView.layout ?: return null
        if (textLayout.lineCount <= 0) return null
        //Layout 只认去掉内边距和滚动后的坐标
        val x = event.x - textView.totalPaddingLeft + textView.scrollX
        val y = event.y - textView.totalPaddingTop + textView.scrollY
        val line = textLayout.getLineForVertical(y.toInt())
        val offset = textLayout.getOffsetForHorizontal(line, x)
        val text = textView.text
        for (index in intArrayOf(offset, offset - 1, offset + 1)) {
            if (index < 0 || index >= text.length) continue
            val entry = ReadingDict.lookup(text, index)
            if (entry != null) return entry
        }
        return null
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val item = items[position]
        //同一章重绑（改字号/行距）不清滚动位置，换章才回到顶部
        val sameChapter = holder.bound && holder.seq == item.seq
        holder.seq = item.seq
        holder.bound = true
        holder.binding.tvChapterTitle.text = item.displayTitle()
        val chapter = chapters[item.seq]
        holder.binding.tvChapterContent.text = chapter?.let { ReadingDict.mark(contentOf(it)) }
            ?: "加载中…"
        applyStyle(holder)
        holder.binding.root.setOnScrollChangeListener { _, _, _, _, _ ->
            settleHolder = holder
            settleHandler.removeCallbacks(settleTask)
            settleHandler.postDelayed(settleTask, SCROLL_SETTLE_MS)
        }
        //换页后正文回到顶部
        if (!sameChapter) holder.binding.root.scrollTo(0, 0)
    }

    /**
     * 按设置取简体或繁体正文。老缓存没有繁体时退回简体，别给个空白页
     */
    private fun contentOf(chapter: BookChapter): String {
        if (setting?.useTrad() == true && chapter.contentTrad.isNotEmpty()) {
            return chapter.contentTrad
        }
        return chapter.content
    }

    /**
     * 把当前的显示设置刷到这一页上
     */
    private fun applyStyle(holder: PageHolder) {
        val style = setting ?: return
        val context = holder.binding.root.context
        val dp = context.resources.displayMetrics.density
        holder.binding.root.setBackgroundColor(style.bgColor())
        holder.binding.tvChapterTitle.setTextColor(style.titleColor())
        holder.binding.tvChapterContent.setTextColor(style.contentTextColorOnBg())
        holder.binding.tvChapterContent.textSize = style.contentTextSize()
        holder.binding.tvChapterContent.setLineSpacing(style.contentLineSpacing() * dp, 1f)
    }

    override fun onViewRecycled(holder: PageHolder) {
        if (settleHolder === holder) {
            settleHandler.removeCallbacks(settleTask)
            settleHolder = null
        }
    }

    /**
     * 章节正文到货，只刷新对应那一页
     */
    fun submitChapter(chapter: BookChapter) {
        val index = items.indexOfFirst { it.seq == chapter.seq }
        if (index < 0) return
        chapters[chapter.seq] = chapter
        notifyItemChanged(index)
    }

    /**
     * 目录被网络结果替换。数据没变就不动，避免翻页位置被重置
     */
    fun submitCatalog(newItems: List<BookChapterItem>) {
        if (items.size == newItems.size && items.firstOrNull()?.seq == newItems.firstOrNull()?.seq
            && items.lastOrNull()?.seq == newItems.lastOrNull()?.seq
        ) {
            return
        }
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getChapter(seq: Int): BookChapter? = chapters[seq]

    fun getItem(position: Int): BookChapterItem? = items.getOrNull(position)

    fun indexOfSeq(seq: Int): Int = items.indexOfFirst { it.seq == seq }

    /**
     * 把指定章滚到上次读到的字符位置，正文渲染完一帧后才执行
     */
    fun scrollToOffset(seq: Int, offset: Int) {
        if (offset <= 0) return
        val rv = attachedRv ?: return
        //刚改完字号时 holder 还在重绑，等这一帧布局完再找
        rv.post find@{
            val index = indexOfSeq(seq)
            if (index < 0) return@find
            val holder = rv.findViewHolderForAdapterPosition(index) as? PageHolder ?: return@find
            holder.binding.tvChapterContent.post inner@{
                if (holder.seq != seq) return@inner
                val layout = holder.binding.tvChapterContent.layout ?: return@inner
                if (offset >= layout.text.length) return@inner
                val line = layout.getLineForOffset(offset)
                holder.binding.root.scrollTo(
                    0, holder.binding.tvChapterContent.top + layout.getLineTop(line)
                )
            }
        }
    }
}

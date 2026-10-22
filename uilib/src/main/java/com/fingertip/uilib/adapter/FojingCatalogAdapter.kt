package com.fingertip.uilib.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.BookChapterItem
import com.fingertip.uilib.databinding.ItemFojingCatalogBinding
import com.fingertip.uilib.databinding.ItemFojingCatalogJuanBinding


/**
 * 目录列表，按卷折叠。
 * 默认只展开当前章所在那一卷——华严八十卷五百多章，全展开根本找不到地方
 */
class FojingCatalogAdapter(
    private val items: List<BookChapterItem>,
    private val currentIndex: Int,
    private val onSelect: (index: Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_JUAN = 0
        private const val TYPE_CHAPTER = 1
    }

    private class JuanRow(val juanNo: Int, val title: String, val count: Int)

    private class ChapterRow(val index: Int, val item: BookChapterItem)

    private class JuanHolder(val binding: ItemFojingCatalogJuanBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class ChapterHolder(val binding: ItemFojingCatalogBinding) :
        RecyclerView.ViewHolder(binding.root)

    //扁平成一维：卷头 + 该卷下展开的章
    private val rows = ArrayList<Any>()

    private val expanded = HashSet<Int>()

    init {
        val current = items.getOrNull(currentIndex) ?: items.firstOrNull()
        current?.let { expanded.add(it.juanNo) }
        rebuild()
    }

    override fun getItemCount() = rows.size

    override fun getItemViewType(position: Int): Int {
        return if (rows[position] is JuanRow) TYPE_JUAN else TYPE_CHAPTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_JUAN) {
            JuanHolder(ItemFojingCatalogJuanBinding.inflate(inflater, parent, false))
        } else {
            ChapterHolder(ItemFojingCatalogBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is JuanRow -> bindJuan(holder as JuanHolder, row)
            is ChapterRow -> bindChapter(holder as ChapterHolder, row)
        }
    }

    private fun bindJuan(holder: JuanHolder, row: JuanRow) {
        holder.binding.tvJuanTitle.text = if (row.title.isNotEmpty()) row.title else "卷${row.juanNo}"
        holder.binding.tvJuanCount.text = "${row.count}章"
        val open = expanded.contains(row.juanNo)
        holder.binding.tvArrow.text = if (open) "▾" else "▸"
        holder.binding.root.setOnClickListener {
            if (open) expanded.remove(row.juanNo) else expanded.add(row.juanNo)
            rebuild()
            notifyDataSetChanged()
        }
    }

    private fun bindChapter(holder: ChapterHolder, row: ChapterRow) {
        holder.binding.tvTitle.text = row.item.displayTitle()
        holder.binding.tvCount.text = "${row.item.charCount}字"
        //正在读的这一章标个色，翻回目录一眼能看到自己在哪
        val reading = row.index == currentIndex
        holder.binding.tvTitle.setTextColor(if (reading) 0xFF8C6B3F.toInt() else 0xFF2C1900.toInt())
        holder.binding.root.setOnClickListener { onSelect(row.index) }
    }

    private fun rebuild() {
        rows.clear()
        var start = 0
        while (start < items.size) {
            val juanNo = items[start].juanNo
            var end = start
            while (end < items.size && items[end].juanNo == juanNo) end++
            rows.add(JuanRow(juanNo, items[start].juanTitle, end - start))
            if (expanded.contains(juanNo)) {
                for (i in start until end) {
                    rows.add(ChapterRow(i, items[i]))
                }
            }
            start = end
        }
    }

    /**
     * 当前章在扁平列表里的位置。它所在的卷如果被折叠了，先展开
     */
    fun positionOfChapter(chapterIndex: Int): Int {
        val juanNo = items.getOrNull(chapterIndex)?.juanNo ?: return -1
        if (!expanded.contains(juanNo)) {
            expanded.add(juanNo)
            rebuild()
            notifyDataSetChanged()
        }
        return rows.indexOfFirst { it is ChapterRow && it.index == chapterIndex }
    }
}

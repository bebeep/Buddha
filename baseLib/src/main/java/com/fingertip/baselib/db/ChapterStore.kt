package com.fingertip.baselib.db

import com.fingertip.baselib.bean.BookChapter
import com.fingertip.baselib.bean.BookChapterItem

/**
 * 章节缓存读写。UI 只从 Room 取数据，网络结果先落库再渲染，
 * 保证在线/离线走同一条渲染路径
 */
object ChapterStore {

    private val dao get() = BuddhaDatabase.get().bookDao()

    /*************************** 目录 ***************************/

    suspend fun getCatalog(bookId: Int): List<BookChapterItem> {
        return dao.getCatalog(bookId).map {
            BookChapterItem().apply {
                seq = it.seq
                juanNo = it.juanNo
                juanTitle = it.juanTitle
                sectionKind = it.sectionKind
                sectionTitle = it.sectionTitle
                partNo = it.partNo
                charCount = it.charCount
                contentHash = it.contentHash
            }
        }
    }

    suspend fun saveCatalog(bookId: Int, items: List<BookChapterItem>) {
        dao.saveCatalog(items.map {
            CatalogEntity(
                bookId = bookId,
                seq = it.seq,
                juanNo = it.juanNo,
                juanTitle = it.juanTitle,
                sectionKind = it.sectionKind,
                sectionTitle = it.sectionTitle,
                partNo = it.partNo,
                charCount = it.charCount,
                contentHash = it.contentHash
            )
        })
    }

    /*************************** 正文 ***************************/

    suspend fun getChapter(bookId: Int, seq: Int): BookChapter? {
        val entity = dao.getChapter(bookId, seq) ?: return null
        return BookChapter().apply {
            this.bookId = entity.bookId
            this.seq = entity.seq
            juanNo = entity.juanNo
            juanTitle = entity.juanTitle
            sectionKind = entity.sectionKind
            sectionTitle = entity.sectionTitle
            partNo = entity.partNo
            content = entity.content
            contentTrad = entity.contentTrad
            charCount = entity.charCount
            charStart = entity.charStart
            charEnd = entity.charEnd
            contentHash = entity.contentHash
        }
    }

    suspend fun saveChapter(chapter: BookChapter) {
        dao.saveChapter(
            ChapterEntity(
                bookId = chapter.bookId,
                seq = chapter.seq,
                juanNo = chapter.juanNo,
                juanTitle = chapter.juanTitle,
                sectionKind = chapter.sectionKind,
                sectionTitle = chapter.sectionTitle,
                partNo = chapter.partNo,
                content = chapter.content,
                contentTrad = chapter.contentTrad,
                charCount = chapter.charCount,
                charStart = chapter.charStart,
                charEnd = chapter.charEnd,
                contentHash = chapter.contentHash,
                cachedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getChapterHash(bookId: Int, seq: Int): String? {
        return dao.getChapterHash(bookId, seq)
    }

    /**
     * 已缓存的章号，整部下载靠它算还缺哪些
     */
    suspend fun getCachedSeqs(bookId: Int): List<Int> {
        return dao.getCachedSeqs(bookId)
    }

    /**
     * 已缓存章节的指纹，一次性取出来跟目录比对，避免逐章查库
     */
    suspend fun getCachedHashes(bookId: Int): Map<Int, String> {
        return dao.getCachedSeqHashes(bookId).associate { it.seq to it.contentHash }
    }

    /*************************** 读经进度 ***************************/

    suspend fun getReadProgress(bookId: Int): ReadProgressEntity? {
        return dao.getReadProgress(bookId)
    }

    suspend fun saveReadProgress(bookId: Int, seq: Int, offset: Int) {
        dao.saveReadProgress(
            ReadProgressEntity(
                bookId = bookId,
                lastReadSeq = seq,
                lastReadOffset = offset,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /*************************** 抄经进度 ***************************/

    suspend fun getCopyProgress(bookId: Int): CopyProgressEntity? {
        return dao.getCopyProgress(bookId)
    }

    suspend fun saveCopyProgress(bookId: Int, seq: Int, offset: Int, copiedChars: Long) {
        dao.saveCopyProgress(
            CopyProgressEntity(
                bookId = bookId,
                lastCopySeq = seq,
                lastCopyOffset = offset,
                copiedChars = copiedChars,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

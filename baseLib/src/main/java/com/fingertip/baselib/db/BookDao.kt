package com.fingertip.baselib.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BookDao {

    /*************************** 目录 ***************************/

    @Query("SELECT * FROM book_catalog WHERE bookId = :bookId ORDER BY seq")
    suspend fun getCatalog(bookId: Int): List<CatalogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCatalog(items: List<CatalogEntity>)

    @Query("DELETE FROM book_catalog WHERE bookId = :bookId")
    suspend fun clearCatalog(bookId: Int)

    /*************************** 正文 ***************************/

    @Query("SELECT * FROM book_chapter WHERE bookId = :bookId AND seq = :seq LIMIT 1")
    suspend fun getChapter(bookId: Int, seq: Int): ChapterEntity?

    @Query("SELECT contentHash FROM book_chapter WHERE bookId = :bookId AND seq = :seq LIMIT 1")
    suspend fun getChapterHash(bookId: Int, seq: Int): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChapter(chapter: ChapterEntity)

    @Query("SELECT COUNT(*) FROM book_chapter WHERE bookId = :bookId")
    suspend fun getCachedChapterCount(bookId: Int): Int

    @Query("SELECT seq FROM book_chapter WHERE bookId = :bookId ORDER BY seq")
    suspend fun getCachedSeqs(bookId: Int): List<Int>

    @Query("SELECT seq, contentHash FROM book_chapter WHERE bookId = :bookId")
    suspend fun getCachedSeqHashes(bookId: Int): List<SeqHash>

    @Query("DELETE FROM book_chapter WHERE bookId = :bookId")
    suspend fun clearChapters(bookId: Int)

    /*************************** 读经进度 ***************************/

    @Query("SELECT * FROM book_read_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun getReadProgress(bookId: Int): ReadProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadProgress(entity: ReadProgressEntity)

    /*************************** 抄经进度 ***************************/

    @Query("SELECT * FROM book_copy_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun getCopyProgress(bookId: Int): CopyProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCopyProgress(entity: CopyProgressEntity)
}

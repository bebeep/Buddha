package com.fingertip.baselib.db

import androidx.room.Entity

/**
 * 本地抄经进度，一部经一行。
 * 抄经是逐字往前的，只需记「抄到哪」和「累计抄了多少字」
 */
@Entity(tableName = "book_copy_progress", primaryKeys = ["bookId"])
data class CopyProgressEntity(
    val bookId: Int,
    val lastCopySeq: Int,
    val lastCopyOffset: Int,
    val copiedChars: Long,
    val updatedAt: Long
)

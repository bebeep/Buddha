package com.fingertip.baselib.db

import androidx.room.Entity

/**
 * 本地读经进度，一部经一行。
 * 只做断网兜底和秒级落盘，真正的进度以服务端 book_reach_record 为准
 */
@Entity(tableName = "book_read_progress", primaryKeys = ["bookId"])
data class ReadProgressEntity(
    val bookId: Int,
    val lastReadSeq: Int,
    val lastReadOffset: Int,
    val updatedAt: Long
)

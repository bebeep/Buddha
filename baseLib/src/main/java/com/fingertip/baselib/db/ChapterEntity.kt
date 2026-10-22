package com.fingertip.baselib.db

import androidx.room.Entity

/**
 * 正文缓存，一章一行。content 原样存服务端字符串，不做任何加工
 */
@Entity(tableName = "book_chapter", primaryKeys = ["bookId", "seq"])
data class ChapterEntity(
    val bookId: Int,
    val seq: Int,
    val juanNo: Int,
    val juanTitle: String,
    val sectionKind: String,
    val sectionTitle: String,
    val partNo: Int,
    val content: String,
    val contentTrad: String,
    val charCount: Int,
    val charStart: Int,
    val charEnd: Int,
    val contentHash: String,
    val cachedAt: Long
)

/**
 * 已缓存章节的指纹，用来跟服务端目录比对，判断哪些章该重下
 */
data class SeqHash(
    val seq: Int,
    val contentHash: String
)

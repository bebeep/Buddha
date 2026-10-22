package com.fingertip.baselib.db

import androidx.room.Entity

/**
 * 目录缓存。只存定位与展示字段，和正文明细分开：
 * 目录轻（557 章约 75KB），正文重，缓存策略不一样，合表会让「有没有正文」判断失真
 */
@Entity(tableName = "book_catalog", primaryKeys = ["bookId", "seq"])
data class CatalogEntity(
    val bookId: Int,
    val seq: Int,
    val juanNo: Int,
    val juanTitle: String,
    val sectionKind: String,
    val sectionTitle: String,
    val partNo: Int,
    val charCount: Int,
    val contentHash: String
)

package com.fingertip.baselib.bean

/**
 * 读经／抄经进度，对应服务端 book_reach_record
 */
class BookProgress {
    var bookId: Int = 0
    //阅读百分比
    var readPercent: Int = 0
    //抄写百分比
    var copyPercent: Int = 0
    //最后阅读章节，0表示没读过
    var lastReadSeq: Int = 0
    //阅读位置：章内字符偏移
    var lastReadOffset: Int = 0
    //最后抄写章节，0表示没抄过
    var lastCopySeq: Int = 0
    //抄写位置：章内字符偏移
    var lastCopyOffset: Int = 0
    //累计抄写字数（含重复抄写）
    var copiedChars: Long = 0
    //累计跳过格数
    var skippedChars: Int = 0
    //全经字数
    var totalChars: Int = 0
    //是否已加入书架
    var isInShelf: Boolean = false
}

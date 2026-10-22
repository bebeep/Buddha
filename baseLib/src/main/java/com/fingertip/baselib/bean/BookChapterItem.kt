package com.fingertip.baselib.bean

/**
 * 章节目录项，对应服务端 book_text_content 的定位字段，不含正文
 */
class BookChapterItem {
    //章节序号，全经从1连续
    var seq: Int = 0
    //卷号
    var juanNo: Int = 0
    //卷题
    var juanTitle: String = ""
    //小节类型：正文／品／序／附
    var sectionKind: String = ""
    //品名等小节标题
    var sectionTitle: String = ""
    //本小节内第几片
    var partNo: Int = 0
    //字数，含标点不含空白
    var charCount: Int = 0
    //内容指纹
    var contentHash: String = ""

    /**
     * 目录里显示的名字：有品名用品名，没有就用卷题
     */
    fun displayTitle(): String {
        if (sectionTitle.isNotEmpty()) return sectionTitle
        if (juanTitle.isNotEmpty()) return juanTitle
        return "第${seq}章"
    }
}

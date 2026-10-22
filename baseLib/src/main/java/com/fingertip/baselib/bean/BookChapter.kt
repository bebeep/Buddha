package com.fingertip.baselib.bean

/**
 * 单章正文
 */
class BookChapter {
    var bookId: Int = 0
    var seq: Int = 0
    var juanNo: Int = 0
    var juanTitle: String = ""
    //正文／品／序／附
    var sectionKind: String = ""
    var sectionTitle: String = ""
    var partNo: Int = 0
    //简体正文
    var content: String = ""
    //繁体正文
    var contentTrad: String = ""
    var charCount: Int = 0
    var charStart: Int = 0
    var charEnd: Int = 0
    var contentHash: String = ""
    //上一章seq，0表示已是第一章
    var prevSeq: Int = 0
    //下一章seq，0表示已是最后一章
    var nextSeq: Int = 0
    //本地指纹一致，服务端没有下发正文
    var notModified: Boolean = false

    fun displayTitle(): String {
        if (sectionTitle.isNotEmpty()) return sectionTitle
        if (juanTitle.isNotEmpty()) return juanTitle
        return "第${seq}章"
    }
}

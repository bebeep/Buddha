package com.fingertip.baselib.bean

class BuddhaBook: TopData() {
    //"经书id"
    var bookId: Int = 0
    //"标题"
    var bookTitle: String = ""
    //"作者"
    var bookAuthor: String = ""
    //"简介"
    var bookIntro: String = ""
    //"封面"
    var bookCover: String = ""
    //"txt下载地址"
    var bookUrl: String = ""
    //阅读进度
    var readPercent:Int = 0
    //抄写进度
    var copyPercent:Int = 0
    //是否在书架
    var isInShelf:Boolean = false
    //"更新时间"
    var updateContentDate: String = "2027-01-01 00:00:00"
}
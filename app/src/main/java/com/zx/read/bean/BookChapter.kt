package com.zx.read.bean


data class BookChapter(
    val chapterId: Long,
    val bookId: Long,
    val chapterIndex: Int,
    var chapterName: String,
    val createTimeValue: Long,
    val updateDate: String,
    val updateTimeValue: Long,
    val chapterUrl: String?,
)
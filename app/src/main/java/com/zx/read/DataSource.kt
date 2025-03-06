package com.zx.read

import com.zx.read.bean.TextChapter

interface DataSource {

    val pageIndex: Int get() = ReadBook.durChapterPos()

    val currentChapter: TextChapter?

    val nextChapter: TextChapter?

    val prevChapter: TextChapter?

    fun hasNextChapter(): Boolean

    fun hasPrevChapter(): Boolean

    fun upContent(relativePosition: Int = 0, resetPageOffset: Boolean = true)
}
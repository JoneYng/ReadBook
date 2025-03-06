package com.zx.read.bean

import kotlin.math.min

/**
 * 图书章节
 */
data class TextChapter(
    val position: Int,//章节位置
    val title: String,//章节标题
    val chapterId: Int,//章节id
    val pages: List<TextPage>,//章节内容
    val pageLines: List<Int>,//
    val pageLengths: List<Int>,
    val chaptersSize: Int//章节大小
) {
    fun page(index: Int): TextPage? {
        return pages.getOrNull(index)
    }

    val lastPage: TextPage? get() = pages.lastOrNull()

    val lastIndex: Int get() = pages.lastIndex

    val pageSize: Int get() = pages.size

    fun isLastIndex(index: Int): Boolean {
        return index >= pages.size - 1
    }

    fun getReadLength(pageIndex: Int): Int {
        var length = 0
        val maxIndex = min(pageIndex, pages.size)
        for (index in 0 until maxIndex) {
            length += pageLengths[index]
        }
        return length
    }

    fun getUnRead(pageIndex: Int): String {
        val stringBuilder = StringBuilder()
        if (pages.isNotEmpty()) {
            for (index in pageIndex..pages.lastIndex) {
                stringBuilder.append(pages[index].text)
            }
        }
        return stringBuilder.toString()
    }

    fun getContent(): String {
        val stringBuilder = StringBuilder()
        pages.forEach {
            stringBuilder.append(it.text)
        }
        return stringBuilder.toString()
    }
}
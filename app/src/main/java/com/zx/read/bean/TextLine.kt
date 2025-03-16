package com.zx.read.bean

import android.text.TextPaint
import com.zx.read.provider.ChapterProvider
import com.zx.read.provider.ChapterProvider.textHeight

/**
 * 每一行的文字
 */
data class TextLine(
    var text: String = "",//一行的文字
    val textChars: ArrayList<TextChar> = arrayListOf(),//一行的字符
    var lineTop: Float = 0f,//一行文字距上的高度
    var lineBase: Float = 0f,//一行文字的基线
    var lineBottom: Float = 0f,//一行文字距下的高度
    val isTitle: Boolean = false,//是否是标题
    val isImage: Boolean = false,//是否是图片
    val isLayout: Boolean = false,//是否是布局
    var isReadAloud: Boolean = false
) {

    fun upTopBottom(durY: Float, textPaint: TextPaint) {
        lineTop = ChapterProvider.paddingTop + durY
        lineBottom = lineTop + textPaint.textHeight
        lineBase = lineBottom - textPaint.fontMetrics.descent
    }

    fun addTextChar(charData: String, start: Float, end: Float) {
        textChars.add(TextChar(charData, start = start, end = end))
    }

    fun getTextCharAt(index: Int): TextChar {
        return textChars[index]
    }

    fun getTextCharReverseAt(index: Int): TextChar {
        return textChars[textChars.lastIndex - index]
    }

    fun getTextCharsCount(): Int {
        return textChars.size
    }
}

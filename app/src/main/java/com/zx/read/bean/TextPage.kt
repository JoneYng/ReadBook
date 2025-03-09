package com.zx.read.bean

import android.text.Layout
import android.text.StaticLayout
import com.zx.read.config.ReadBookConfig
import com.zx.read.extensions.dp
import com.zx.read.provider.ChapterProvider
import java.text.DecimalFormat


/**
 * @description:
 * @author: zhouxiang
 * @created: 2025/03/05 11:17
 * @version: V1.0
 */
data class TextPage(
    var index: Int = 0,
    //当前页面的数据
    var text: String = "加载中...",
    //一页的标题
    var title: String = "",
    //一个页面的所有行
    val textLines: ArrayList<TextLine> = arrayListOf(),
    //页码大小
    var pageSize: Int = 0,
    //章节大小
    var chapterSize: Int = 0,
    //章节位置
    var chapterIndex: Int = 0,
    //页码高度
    var height: Float = 0f
) {
    fun upLinesPosition() = ChapterProvider.apply {
        if (!ReadBookConfig.textBottomJustify) return@apply
        if (textLines.size <= 1) return@apply
        if (textLines.last().isImage) return@apply
        if (visibleHeight - height >= with(textLines.last()) { lineBottom - lineTop }) return@apply
        val surplus = (visibleBottom - textLines.last().lineBottom - ReadBookConfig.paddingBottom.dp)
        if (surplus == 0f) return@apply
        height += surplus
        val tj = surplus / (textLines.size - 1)
        for (i in 1 until textLines.size) {
            val line = textLines[i]
            line.lineTop = line.lineTop + tj * i
            line.lineBase = line.lineBase + tj * i
            line.lineBottom = line.lineBottom + tj * i
        }
    }

    @Suppress("DEPRECATION")
    fun format(): TextPage {
        //当前页码行为空
        if (textLines.isEmpty() && ChapterProvider.visibleWidth > 0) {
            val layout = StaticLayout(
                text,
                ChapterProvider.contentPaint,
                ChapterProvider.visibleWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1f,
                0f,
                false
            )
            var y = (ChapterProvider.visibleHeight - layout.height) / 2f
            if (y < 0) y = 0f
            for (lineIndex in 0 until layout.lineCount) {
                val textLine = TextLine()
                textLine.lineTop = ChapterProvider.paddingTop + y + layout.getLineTop(lineIndex)
                textLine.lineBase =
                    ChapterProvider.paddingTop + y + layout.getLineBaseline(lineIndex)
                textLine.lineBottom =
                    ChapterProvider.paddingTop + y + layout.getLineBottom(lineIndex)
                var x =
                    ChapterProvider.paddingLeft + (ChapterProvider.visibleWidth - layout.getLineMax(
                        lineIndex
                    )) / 2
                textLine.text =
                    text.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
                for (i in textLine.text.indices) {
                    val char = textLine.text[i].toString()
                    val cw = StaticLayout.getDesiredWidth(char, ChapterProvider.contentPaint)
                    val x1 = x + cw
                    textLine.addTextChar(charData = char, start = x, end = x1)
                    x = x1
                }
                textLines.add(textLine)
            }
            height = ChapterProvider.visibleHeight.toFloat()
        }
        return this
    }

    fun removePageAloudSpan(): TextPage {
        textLines.forEach { textLine ->
            textLine.isReadAloud = false
        }
        return this
    }

    val readProgress: String
        get() {
            val df = DecimalFormat("0.0%")
            if (chapterSize == 0 || pageSize == 0 && chapterIndex == 0) {
                return "0.0%"
            } else if (pageSize == 0) {
                return df.format((chapterIndex + 1.0f) / chapterSize.toDouble())
            }
            var percent =
                df.format(chapterIndex * 1.0f / chapterSize + 1.0f / chapterSize * (index + 1) / pageSize.toDouble())
            if (percent == "100.0%" && (chapterIndex + 1 != chapterSize || index + 1 != pageSize)) {
                percent = "99.9%"
            }
            return percent
        }
}
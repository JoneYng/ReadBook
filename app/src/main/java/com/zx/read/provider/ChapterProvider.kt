package com.zx.read.provider

import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.zx.read.App
import com.zx.read.AppPattern
import com.zx.read.bean.Book
import com.zx.read.bean.BookChapter
import com.zx.read.bean.TextChapter
import com.zx.read.bean.TextChar
import com.zx.read.bean.TextLine
import com.zx.read.bean.TextPage
import com.zx.read.config.AppConfig
import com.zx.read.config.ReadBookConfig
import com.zx.read.extensions.dp
import com.zx.read.extensions.sp
import com.zx.read.extensions.toStringArray
import com.zx.read.utils.NetworkUtils
import java.util.ArrayList
import java.util.Locale


/**
 * @description:
 * @author: zhouxiang
 * @created: 2025/03/05 11:13
 * @version: V1.0
 */
object ChapterProvider {
    private var viewWidth = 0//文字view的宽度
    private var viewHeight = 0//文字view的高度
    var paddingLeft = 0
    var paddingTop = 0
    var visibleWidth = 0//显示宽度
    var visibleHeight = 0//显示高度
    var visibleRight = 0
    var visibleBottom = 0
    private var lineSpacingExtra = 0//行间距
    private var paragraphSpacing = 0//段落间距

    private var titleTopSpacing = 0 //标题上间距
    private var titleBottomSpacing = 0//标题下间距

    var typeface: Typeface = Typeface.SANS_SERIF
    lateinit var titlePaint: TextPaint//文章标题画笔
    lateinit var contentPaint: TextPaint//文章正文画笔

    val TextPaint.textHeight: Float
        get() = fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading


    init {
        upStyle()
    }
    fun String?.isContentPath(): Boolean = this?.startsWith("content://") == true

    fun upStyle() {
        typeface = try {
            val fontPath = ReadBookConfig.textFont
            when {
                fontPath.isContentPath() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val fd = App.INSTANCE.contentResolver
                        .openFileDescriptor(Uri.parse(fontPath), "r")!!
                        .fileDescriptor
                    Typeface.Builder(fd).build()
                }
//                fontPath.isContentPath() -> {
//                    Typeface.createFromFile(RealPathUtil.getPath(App.INSTANCE, Uri.parse(fontPath)))
//                }
                fontPath.isNotEmpty() -> Typeface.createFromFile(fontPath)
                else -> when (AppConfig.systemTypefaces) {
                    1 -> Typeface.SERIF
                    2 -> Typeface.MONOSPACE
                    else -> Typeface.SANS_SERIF
                }
            }
        } catch (e: Exception) {
            ReadBookConfig.textFont = ""
            ReadBookConfig.save()
            Typeface.SANS_SERIF
        }
        // 字体统一处理
        val bold = Typeface.create(typeface, Typeface.BOLD)
        val normal = Typeface.create(typeface, Typeface.NORMAL)
        val (titleFont, textFont) = when (ReadBookConfig.textBold) {
            1 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    Pair(Typeface.create(typeface, 900, false), bold)
                else
                    Pair(bold, bold)
            }
            2 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    Pair(normal, Typeface.create(typeface, 300, false))
                else
                    Pair(normal, normal)
            }
            else -> Pair(bold, normal)
        }

        //标题
        titlePaint = TextPaint()
        titlePaint.color = ReadBookConfig.textColor
        titlePaint.letterSpacing = ReadBookConfig.letterSpacing
        titlePaint.typeface = titleFont
        titlePaint.textSize = with(ReadBookConfig) { textSize + titleSize }.sp.toFloat()
        titlePaint.isAntiAlias = true
        //正文
        contentPaint = TextPaint()
        contentPaint.color = ReadBookConfig.textColor
        contentPaint.letterSpacing = ReadBookConfig.letterSpacing
        contentPaint.typeface = textFont
        contentPaint.textSize = ReadBookConfig.textSize.sp.toFloat()
        contentPaint.isAntiAlias = true
        //间距
        lineSpacingExtra = ReadBookConfig.lineSpacingExtra
        paragraphSpacing = ReadBookConfig.paragraphSpacing
        titleTopSpacing = ReadBookConfig.titleTopSpacing.dp
        titleBottomSpacing = ReadBookConfig.titleBottomSpacing.dp
        upVisibleSize()
    }


    /**
     * 获取拆分完的章节数据
     */
    fun getTextChapter(
        book: Book,
        bookChapter: BookChapter,
        contents: List<String>,
        chapterSize: Int,
        imageStyle: String?,
    ): TextChapter {
        //章节所有页面
        val textPages = arrayListOf<TextPage>()
        val pageLines = arrayListOf<Int>()
        val pageLengths = arrayListOf<Int>()
        val stringBuilder = StringBuilder()
        var durY = 0f
        //添加一个空页面
        textPages.add(TextPage())
        contents.forEachIndexed { index, text ->
            val matcher = AppPattern.imgPattern.matcher(text)
            if (matcher.find()) {
                var src = matcher.group(1)
                src = NetworkUtils.getAbsoluteURL("", src)
                src?.let {
                    durY = setTypeImage(
                        book, bookChapter, src, durY, textPages, imageStyle
                    )
                }
            } else {
                val isTitle = index == 0
                if (!(isTitle && ReadBookConfig.titleMode == 2)) {
                    durY =
                        setTypeText(
                            text, durY, textPages, pageLines,
                            pageLengths, stringBuilder, isTitle
                        )
                }
            }
        }
        //获取 textPages 列表的最后一个元素
        textPages.last().height = durY + 20.dp
        textPages.last().text = stringBuilder.toString()
        if (pageLines.size < textPages.size) {
            pageLines.add(textPages.last().textLines.size)
        }
        if (pageLengths.size < textPages.size) {
            pageLengths.add(textPages.last().text.length)
        }
        textPages.forEachIndexed { index, item ->
            item.index = index
            item.pageSize = textPages.size
            item.chapterIndex = bookChapter.chapterIndex
            item.chapterSize = chapterSize
            item.title = bookChapter.chapterName
            item.upLinesPosition()
        }

        return TextChapter(
            bookChapter.chapterIndex,
            bookChapter.chapterName,
            bookChapter.chapterId.toInt(),
            textPages,
            pageLines,
            pageLengths,
            chapterSize
        )
    }
    /**
     * 排版图片
     */
    private fun setTypeImage(
        book: Book,
        chapter: BookChapter,
        src: String,
        y: Float,
        textPages: ArrayList<TextPage>,
        imageStyle: String?,
    ): Float {
        var durY = y
        ImageProvider.getImage(book, chapter.chapterId.toInt(), src)?.let {
            //当前偏移高度大于显示高度
            if (durY > visibleHeight) {
                textPages.last().height = durY
                textPages.add(TextPage())
                durY = 0f
            }
            var height = it.height
            var width = it.width
            when (imageStyle?.toUpperCase(Locale.ROOT)) {
                "FULL" -> {
                    width = visibleWidth
                    height = it.height * visibleWidth / it.width
                }
                else -> {
                    if (it.width > visibleWidth) {
                        height = it.height * visibleWidth / it.width
                        width = visibleWidth
                    }
                    if (height > visibleHeight) {
                        width = width * visibleHeight / height
                        height = visibleHeight
                    }
                    //图片高度+当前高度>显示高度
                    if (durY + height > visibleHeight) {
                        textPages.last().height = durY
                        textPages.add(TextPage())
                        durY = 0f
                    }
                }
            }
            val textLine = TextLine(isImage = true)
            textLine.lineTop = durY
            durY += height
            textLine.lineBottom = durY
            val (start, end) = if (visibleWidth > width) {
                val adjustWidth = (visibleWidth - width) / 2f
                Pair(
                    paddingLeft.toFloat() + adjustWidth,
                    paddingLeft.toFloat() + adjustWidth + width
                )
            } else {
                Pair(paddingLeft.toFloat(), (paddingLeft + width).toFloat())
            }
            textLine.textChars.add(
                TextChar(
                    charData = src,
                    start = start,
                    end = end,
                    isImage = true
                )
            )
            textPages.last().textLines.add(textLine)
        }
        return durY + paragraphSpacing / 10f
    }
    /**
     * 排版文字
     */
    private fun setTypeText(
        text: String,
        y: Float,
        textPages: ArrayList<TextPage>,
        pageLines: ArrayList<Int>,
        pageLengths: ArrayList<Int>,
        stringBuilder: StringBuilder,
        isTitle: Boolean,
    ): Float {
        var durY = if (isTitle) y + titleTopSpacing else y
        val textPaint = if (isTitle) titlePaint else contentPaint
        val layout = StaticLayout(
            text, textPaint, visibleWidth, Layout.Alignment.ALIGN_NORMAL, 0f, 0f, true
        )
        for (lineIndex in 0 until layout.lineCount) {
            val textLine = TextLine(isTitle = isTitle)
            val words =
                text.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
            val desiredWidth = layout.getLineWidth(lineIndex)
            var isLastLine = false
            if (lineIndex == 0 && layout.lineCount > 1 && !isTitle) {
                //第一行
                textLine.text = words
                addCharsToLineFirst(
                    textLine,
                    words.toStringArray(),
                    textPaint,
                    desiredWidth
                )
            } else if (lineIndex == layout.lineCount - 1) {
                //最后一行
                textLine.text = "$words\n"
                isLastLine = true
                val x = if (isTitle && ReadBookConfig.titleMode == 1)
                    (visibleWidth - layout.getLineWidth(lineIndex)) / 2
                else 0f
                addCharsToLineLast(
                    textLine,
                    words.toStringArray(),
                    textPaint,
                    x
                )
            } else {
                //中间行
                textLine.text = words
                addCharsToLineMiddle(
                    textLine,
                    words.toStringArray(),
                    textPaint,
                    desiredWidth,
                    0f
                )
            }
            if (durY + textPaint.textHeight > visibleHeight) {
                //当前页面结束,设置各种值
                textPages.last().text = stringBuilder.toString()
                pageLines.add(textPages.last().textLines.size)
                pageLengths.add(textPages.last().text.length)
                textPages.last().height = durY
                //新建页面
                textPages.add(TextPage())
                stringBuilder.clear()
                durY = 0f
            }
            stringBuilder.append(words)
            if (isLastLine) stringBuilder.append("\n")
            textPages.last().textLines.add(textLine)
            textLine.upTopBottom(durY, textPaint)
            durY += textPaint.textHeight * lineSpacingExtra / 10f
            textPages.last().height = durY
        }
        if (isTitle) durY += titleBottomSpacing
        durY += textPaint.textHeight * paragraphSpacing / 10f
        return durY
    }
    /**
     * 有缩进,两端对齐
     */
    private fun addCharsToLineFirst(
        textLine: TextLine,
        words: Array<String>,
        textPaint: TextPaint,
        desiredWidth: Float,
    ) {
        var x = 0f
        if (!ReadBookConfig.textFullJustify) {
            addCharsToLineLast(
                textLine,
                words,
                textPaint,
                x
            )
            return
        }
        val bodyIndent = ReadBookConfig.paragraphIndent
        val icw = StaticLayout.getDesiredWidth(bodyIndent, textPaint) / bodyIndent.length
        bodyIndent.toStringArray().forEach {
            val x1 = x + icw
            textLine.addTextChar(
                charData = it,
                start = paddingLeft + x,
                end = paddingLeft + x1
            )
            x = x1
        }
        val words1 = words.copyOfRange(bodyIndent.length, words.size)
        addCharsToLineMiddle(
            textLine,
            words1,
            textPaint,
            desiredWidth,
            x
        )
    }

    /**
     * 无缩进,两端对齐
     */
    private fun addCharsToLineMiddle(
        textLine: TextLine,
        words: Array<String>,
        textPaint: TextPaint,
        desiredWidth: Float,
        startX: Float,
    ) {
        if (!ReadBookConfig.textFullJustify) {
            addCharsToLineLast(
                textLine,
                words,
                textPaint,
                startX
            )
            return
        }
        val gapCount: Int = words.lastIndex
        val d = (visibleWidth - desiredWidth) / gapCount
        var x = startX
        words.forEachIndexed { index, s ->
            val cw = StaticLayout.getDesiredWidth(s, textPaint)
            val x1 = if (index != words.lastIndex) (x + cw + d) else (x + cw)
            textLine.addTextChar(
                charData = s,
                start = paddingLeft + x,
                end = paddingLeft + x1
            )
            x = x1
        }
        exceed(
            textLine,
            words
        )
    }

    /**
     * 最后一行,自然排列
     */
    private fun addCharsToLineLast(
        textLine: TextLine,
        words: Array<String>,
        textPaint: TextPaint,
        startX: Float,
    ) {
        var x = startX
        words.forEach {
            val cw = StaticLayout.getDesiredWidth(it, textPaint)
            val x1 = x + cw
            textLine.addTextChar(
                charData = it,
                start = paddingLeft + x,
                end = paddingLeft + x1
            )
            x = x1
        }
        exceed(
            textLine,
            words
        )
    }

    /**
     * 超出边界处理
     */
    private fun exceed(textLine: TextLine, words: Array<String>) {
        val endX = textLine.textChars.last().end
        if (endX > visibleRight) {
            val cc = (endX - visibleRight) / words.size
            for (i in 0..words.lastIndex) {
                textLine.getTextCharReverseAt(i).let {
                    val py = cc * (words.size - i)
                    it.start = it.start - py
                    it.end = it.end - py
                }
            }
        }
    }
    /**
     * 更新View尺寸
     */
    fun upViewSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            viewWidth = width
            viewHeight = height
            upVisibleSize()
        }
    }
    /**
     * 更新绘制尺寸
     */
    private fun upVisibleSize() {
        if (viewWidth > 0 && viewHeight > 0) {
            paddingLeft = ReadBookConfig.paddingLeft.dp
            paddingTop = ReadBookConfig.paddingTop.dp
            visibleWidth = viewWidth - paddingLeft - ReadBookConfig.paddingRight.dp
            visibleHeight = viewHeight - paddingTop - ReadBookConfig.paddingBottom.dp
            visibleRight = paddingLeft + visibleWidth
            visibleBottom = paddingTop + visibleHeight
        }
    }

}
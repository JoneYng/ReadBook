package com.zx.read.provider

import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
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
            val imgMatcher = AppPattern.imgPattern.matcher(text)
            val layoutMatcher = AppPattern.layoutPattern.matcher(text)
            if (imgMatcher.find()) {
                var src = imgMatcher.group(1)
                src = NetworkUtils.getAbsoluteURL("", src)
                src?.let {
                    durY = setTypeImage(
                        book, bookChapter, src, durY, textPages, imageStyle
                    )
                }
            } else if(layoutMatcher.find()){
                var tupe = layoutMatcher.group(1)
                Log.i("type", "tupe:$tupe")
                tupe?.let {
                    durY = setTypeLayout( tupe, durY, textPages)
                }
            }else {
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
     * 排版布局
     */
    private fun setTypeLayout(
        layout: String,
        y: Float,
        textPages: ArrayList<TextPage>,
    ): Float {
        var durY = y
        //当前偏移高度大于显示高度
        if (durY > visibleHeight) {
            textPages.last().height = durY
            textPages.add(TextPage())
            durY = 0f
        }
        var height = 200f.toInt()
        var width = 100f.toInt()
        //布局高度+当前高度>显示高度
        if (durY + height > visibleHeight) {
            textPages.last().height = durY
            textPages.add(TextPage())
            durY = 0f
        }
        val textLine = TextLine(isLayout = true)
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
                charData = "",
                start = start,
                end = end,
                isImage = true
            )
        )
        textPages.last().textLines.add(textLine)
        return durY + paragraphSpacing / 10f
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


    // 扩展函数：条件追加字符串
    private fun StringBuilder.appendIf(condition: Boolean, text: String) {
        if (condition) append(text)
    }

    /**
     * 排版文本内容到指定页面，支持标题与正文的不同格式处理，自动分页
     *
     * @param text 需要排版的原始文本内容
     * @param y 起始Y轴坐标（基于当前页面坐标系）
     * @param textPages 页面数据集合（会被修改）
     * @param pageLines 记录每页行数的集合（会被修改）
     * @param pageLengths 记录每页字符数的集合（会被修改）
     * @param stringBuilder 用于累积当前页文本内容的缓冲区
     * @param isTitle 是否为标题文本（影响排版样式）
     * @return 排版完成后的最新Y轴坐标
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
        // 初始化垂直起始位置（标题增加顶部间距）
        var durY = if (isTitle) y + titleTopSpacing else y
        // 根据文本类型选择画笔
        val textPaint = if (isTitle) titlePaint else contentPaint
        // 创建静态布局处理自动换行（可见区域宽度作为最大行宽)
        val layout = StaticLayout(
            text, textPaint, visibleWidth,
            Layout.Alignment.ALIGN_NORMAL,  // 左对齐
            0f, // 行间距倍数（不使用）
             0f,// 行间距加值（不使用）
            true// 包含内边距
        )
//        // 新增底部间距参数（建议通过资源配置）
//        val bottomMargin = 24f // 单位：像素
//        // 计算实际可用高度
//        val availableHeight = visibleHeight - bottomMargin
        // 逐行处理排版逻辑
        for (lineIndex in 0 until layout.lineCount) {
            val textLine = TextLine(isTitle = isTitle)
            val words =
                text.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
            val desiredWidth = layout.getLineWidth(lineIndex)
            var isLastLine = false
            // 处理首行特殊情况（非标题且多行时）
            if (lineIndex == 0 && layout.lineCount > 1 && !isTitle) {
                textLine.text = words
                addCharsToLineFirst(
                    textLine,
                    words.toStringArray(),
                    textPaint,
                    desiredWidth
                )
            }
            // 处理末行（添加换行符）
            else if (lineIndex == layout.lineCount - 1) {
                textLine.text = "$words\n"
                isLastLine = true
                // 标题居中处理
                val x = if (isTitle && ReadBookConfig.titleMode == 1)
                    (visibleWidth - layout.getLineWidth(lineIndex)) / 2
                else 0f
                addCharsToLineLast(
                    textLine,
                    words.toStringArray(),
                    textPaint,
                    x
                )
            }
            // 普通中间行处理
            else {
                textLine.text = words
                addCharsToLineMiddle(
                    textLine,
                    words.toStringArray(),
                    textPaint,
                    desiredWidth,
                    0f// 首行缩进值
                )
            }


            // 分页检测：当内容超出可视区域高度时
            if (durY + textPaint.textHeight > visibleHeight) {
                // 完成当前页设置
                textPages.last().text = stringBuilder.toString()
                // 记录行数
                pageLines.add(textPages.last().textLines.size)
                // 记录字符数
                pageLengths.add(textPages.last().text.length)
                textPages.last().height = durY
                // 创建新页面并重置状态
                textPages.add(TextPage())
                stringBuilder.clear()
                // 新页Y轴归零
                durY = 0f
            }
            // 累积当前行内容
            stringBuilder.append(words)
            // 末行添加换行
            if (isLastLine) stringBuilder.append("\n")
            // 更新页面数据
            textPages.last().textLines.add(textLine)
            // 设置行位置
            textLine.upTopBottom(durY, textPaint)
            // 更新垂直坐标（考虑行间距系数）
            durY += textPaint.textHeight * lineSpacingExtra / 10f
            // 记录当前页高度
            textPages.last().height = durY
        }
        // 添加段落间距（标题额外增加底部间距）
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
            visibleHeight = viewHeight - paddingTop
//            visibleHeight = viewHeight - paddingTop - ReadBookConfig.paddingBottom.dp
            visibleRight = paddingLeft + visibleWidth
            visibleBottom = paddingTop + visibleHeight
        }
    }

}
package com.zx.read.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.zx.read.provider.ChapterProvider
import com.zx.read.provider.ImageProvider
import com.zx.read.PreferKey
import com.zx.read.ReadBook
import com.zx.read.config.ReadBookConfig
import com.zx.read.factory.TextPageFactory
import com.zx.read.extensions.activity
import com.zx.read.bean.TextLine
import com.zx.read.bean.TextPage
import com.zx.read.config.ReadBookConfig.emptyString
import com.zx.read.extensions.getPrefBoolean
import com.zx.readbook.R
import kotlinx.coroutines.CoroutineScope
import kotlin.math.min

fun Context.getCompatColor(@ColorRes id: Int): Int = ContextCompat.getColor(this, id)

/**
 * @description:阅读内容界面
 * @author: zhouxiang
 * @created: 2025/03/05 11:22
 * @version: V1.0
 */
class ContentTextView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    //是否开启选中
    var selectAble = context.getPrefBoolean(PreferKey.textSelectAble, true)

    //选中文字的画笔
    private val selectedPaint by lazy {
        Paint().apply {
            color = context.getCompatColor(R.color.purple_80)
            style = Paint.Style.FILL
        }
    }

    //显示大小
    private val visibleRect = RectF()

    //选中开始位置
    private val selectStart = arrayOf(0, 0, 0)

    //选中结束位置
    private val selectEnd = arrayOf(0, 0, 0)

    private var textPage: TextPage = TextPage()//

    private var callBack: CallBack

    var upView: ((TextPage) -> Unit)? = null

    //滚动参数
    private val pageFactory: TextPageFactory get() = callBack.pageFactory

    //页面偏移
    private var pageOffset = 0f

    init {
        callBack = activity as CallBack
        contentDescription = textPage.text
    }

    fun setContent(textPage: TextPage) {
        this.textPage = textPage
        contentDescription = textPage.text
        invalidate()
    }


    /**
     * 更新显示区域
     */
    fun upVisibleRect() {
        visibleRect.set(
            ChapterProvider.paddingLeft.toFloat(),
            ChapterProvider.paddingTop.toFloat(),
            ChapterProvider.visibleRight.toFloat(),
            ChapterProvider.visibleBottom.toFloat()
        )
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ChapterProvider.upViewSize(w, h)
        upVisibleRect()
        textPage.format()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 定义裁剪区域 (left, top, right, bottom)
        canvas.clipRect(visibleRect)
        drawPage(canvas)
    }

    /**
     * 绘制页面
     */
    private fun drawPage(canvas: Canvas) {
        var relativeOffset = relativeOffset(0)
        //页面的所有行
        textPage.textLines.forEach { textLine ->
            draw(canvas, textLine, relativeOffset)
        }
        //滚动中
        if (!callBack.isScroll) return
        //是否有下一章
        if (!pageFactory.hasNext()) return
        //下一页
        val nextPage = relativePage(1)
        relativeOffset = relativeOffset(1)
        //页面的所有行
        nextPage.textLines.forEach { textLine ->
            draw(canvas, textLine, relativeOffset)
        }
        //下两页
        if (!pageFactory.hasNextPlus()) return
        relativeOffset = relativeOffset(2)
        if (relativeOffset < ChapterProvider.visibleHeight) {
            relativePage(2).textLines.forEach { textLine ->
                draw(canvas, textLine, relativeOffset)
            }
        }
    }

    private fun draw(
        canvas: Canvas, textLine: TextLine, relativeOffset: Float
    ) {
        val lineTop = textLine.lineTop + relativeOffset
        val lineBase = textLine.lineBase + relativeOffset
        val lineBottom = textLine.lineBottom + relativeOffset
        if (textLine.isImage) {
            drawImage(canvas, textLine, lineTop, lineBottom)
        } else {
            drawChars(
                canvas, lineTop, lineBase, lineBottom, textLine
            )
        }
    }

    /**
     * 绘制文字
     * @param canvas
     * @param lineTop
     * @param lineBase
     * @param lineBottom
     * @param textLine
     */
    private fun drawChars(
        canvas: Canvas, lineTop: Float, lineBase: Float, lineBottom: Float, textLine: TextLine
    ) {
        val textPaint =
            if (textLine.isTitle) ChapterProvider.titlePaint else ChapterProvider.contentPaint
        textPaint.color =
            if (textLine.isReadAloud) ReadBookConfig.textColor else ReadBookConfig.textColor

        var selectionStartX = 0f //开始位置
        var selectionEndX = 0f//结束位置
        var baselineY = 0f
        textLine.textChars.forEachIndexed { index, textChar ->
            // 绘制字符
            canvas.drawText(textChar.charData, textChar.start, lineBase, textPaint)
            if (textChar.selected) {
                canvas.drawRect(textChar.start, lineTop, textChar.end, lineBottom, selectedPaint)
            }
            baselineY = lineBottom
        }
        if(textLine.text.contains("资本主义的发展")){
            try {
                //直线
                drawLine(canvas, textLine.textChars.get(3).start, textLine.textChars.get(10).end, baselineY, LineType.STRAIGHT)
                //直线-虚线
                drawLine(canvas,textLine.textChars.get(12).start, textLine.textChars.get(20).end, baselineY, LineType.DASHED)
                //曲线
                drawLine(canvas, textLine.textChars.get(22).start, textLine.textChars.get(30).end, baselineY, LineType.WAVY)
                //背景
                canvas.drawRect(textLine.textChars.get(32).start, lineTop, textLine.textChars.get(40).end, baselineY, selectedPaint)
            }catch (e:Exception){

            }
        }

    }


    /**
     * 在画布上绘制不同类型的线条
     *
     * @param canvas 画布对象
     * @param startX 线条起始 X 坐标
     * @param endX 线条结束 X 坐标
     * @param y 线条的 Y 坐标
     * @param type 线条类型（直线、虚线、波浪线）
     * @param waveHeight 波浪线的高度（仅 `WAVY` 类型有效，默认 10f）
     * @param waveLength 波浪线的宽度（仅 `WAVY` 类型有效，默认 20f）
     */
    private fun drawLine(
        canvas: Canvas,
        startX: Float,
        endX: Float,
        y: Float,
        type: LineType,
        waveHeight: Float = 10f,
        waveLength: Float = 20f
    ) {
        /**
         * 画笔设置（默认用于直线和波浪线）
         */
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.purple_200)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        when (type) {
            // **绘制直线**
            LineType.STRAIGHT -> {
                canvas.drawLine(startX, y, endX, y, linePaint)
            }

            // **绘制虚线**
            LineType.DASHED -> {
                val dashPaint = Paint(linePaint).apply {
                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f) // 10px 实线，10px 空白
                }
                canvas.drawLine(startX, y, endX, y, dashPaint)
            }

            // **绘制波浪线**
            LineType.WAVY -> {
                val path = Path()
                path.moveTo(startX, y)
                var x = startX
                val adjustedEndX = endX - (endX - startX) % waveLength // 确保波浪完整
                while (x < adjustedEndX) {
                    val midX = x + waveLength / 2
                    path.quadTo(midX, y + waveHeight, x + waveLength, y) // 画二阶贝塞尔曲线
                    x += waveLength
                }
                canvas.drawPath(path, linePaint)
            }
        }
    }


    /**
     * 绘制图片
     */
    private fun drawImage(
        canvas: Canvas, textLine: TextLine, lineTop: Float, lineBottom: Float
    ) {
        textLine.textChars.forEach { textChar ->
            ReadBook.book?.let { book ->
                val rectF = RectF(textChar.start, lineTop, textChar.end, lineBottom)
                ImageProvider.getImage(book, textPage.chapterIndex, textChar.charData, true)?.let {
                        canvas.drawBitmap(it, null, rectF, null)
                    }
            }
        }
    }

    /**
     * 滚动事件
     */
    fun onScroll(mOffset: Float) {
        if (mOffset == 0f) return
        pageOffset += mOffset
        if (!pageFactory.hasPrev() && pageOffset > 0) {
            pageOffset = 0f
        } else if (!pageFactory.hasNext() && pageOffset < 0 && pageOffset + textPage.height < ChapterProvider.visibleHeight) {
            val offset = ChapterProvider.visibleHeight - textPage.height
            pageOffset = min(0f, offset)
        } else if (pageOffset > 0) {
            pageFactory.moveToPrev(false)
            textPage = pageFactory.currentPage
            pageOffset -= textPage.height
            upView?.invoke(textPage)
        } else if (pageOffset < -textPage.height) {
            pageOffset += textPage.height
            pageFactory.moveToNext(false)
            textPage = pageFactory.currentPage
            upView?.invoke(textPage)
        }
        invalidate()
    }

    fun resetPageOffset() {
        pageOffset = 0f
    }

    /**
     * 选择文字
     */
    fun selectText(
        x: Float, y: Float, select: (relativePage: Int, lineIndex: Int, charIndex: Int) -> Unit
    ) {
        if (!selectAble) return
        if (!visibleRect.contains(x, y)) return
        var relativeOffset: Float
        for (relativePos in 0..2) {
            relativeOffset = relativeOffset(relativePos)
            if (relativePos > 0) {
                //滚动翻页
                if (!callBack.isScroll) return
                if (relativeOffset >= ChapterProvider.visibleHeight) return
            }
            val page = relativePage(relativePos)
            for ((lineIndex, textLine) in page.textLines.withIndex()) {
                if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                    for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                        if (x > textChar.start && x < textChar.end) {
                            if (textChar.isImage) {
                                activity?.supportFragmentManager?.let {
//                                    PhotoDialog.show(it, page.chapterIndex, textChar.charData)
                                }
                            } else {
                                textChar.selected = true
                                invalidate()
                                select(relativePos, lineIndex, charIndex)
                            }
                            return
                        }
                    }
                    return
                }
            }

        }
    }

    /**
     * 开始选择符移动
     */
    fun selectStartMove(x: Float, y: Float, startCursorOutOfBounds: () -> Unit) {
        if (!visibleRect.contains(x, y)) return
        var relativeOffset: Float
        for (relativePos in 0..2) {
            relativeOffset = relativeOffset(relativePos)
            if (relativePos > 0) {
                //滚动翻页
                if (!callBack.isScroll) return
                if (relativeOffset >= ChapterProvider.visibleHeight) return
            }
            for ((lineIndex, textLine) in relativePage(relativePos).textLines.withIndex()) {
                if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                    for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                        if (x > textChar.start && x < textChar.end) {
                            //如果是空，不设置选中
                            if(textChar.charData== emptyString){
                                return
                            }
                            if (selectStart[0] != relativePos || selectStart[1] != lineIndex || selectStart[2] != charIndex) {
                                //开始坐标大于结束坐标
                                if (selectToInt(relativePos, lineIndex, charIndex) > selectToInt(
                                        selectEnd
                                    )
                                ) {
                                    startCursorOutOfBounds()
                                    return
                                }
                                selectStart[0] = relativePos
                                selectStart[1] = lineIndex
                                selectStart[2] = charIndex
                                upSelectedStart(
                                    textChar.start,
                                    textLine.lineBottom + relativeOffset,
                                    textLine.lineTop + relativeOffset
                                )
                                upSelectChars()
                            }
                            return
                        }
                    }
                    return
                }
            }
        }
    }

    /**
     * 结束选择符移动
     */
    fun selectEndMove(x: Float, y: Float, endCursorOutOfBounds: () -> Unit) {
        if (!visibleRect.contains(x, y)) return
        var relativeOffset: Float
        for (relativePos in 0..2) {
            relativeOffset = relativeOffset(relativePos)
            if (relativePos > 0) {
                //滚动翻页
                if (!callBack.isScroll) return
                if (relativeOffset >= ChapterProvider.visibleHeight) return
            }

            for ((lineIndex, textLine) in relativePage(relativePos).textLines.withIndex()) {
                if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                    for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                        if (x > textChar.start && x < textChar.end) {
                            if (selectEnd[0] != relativePos || selectEnd[1] != lineIndex || selectEnd[2] != charIndex) {
                                //结束坐标小于开始坐标
                                if (selectToInt(relativePos, lineIndex, charIndex) < selectToInt(
                                        selectStart
                                    )
                                ) {
                                    endCursorOutOfBounds()
                                } else {
                                    selectEnd[0] = relativePos
                                    selectEnd[1] = lineIndex
                                    selectEnd[2] = charIndex
                                    upSelectedEnd(
                                        textChar.end, textLine.lineBottom + relativeOffset
                                    )
                                    upSelectChars()
                                }
                            }
                            return
                        }
                    }
                    return
                }
            }
        }
    }

    /**
     * 选择开始文字
     */
    fun selectStartMoveIndex(relativePage: Int, lineIndex: Int, charIndex: Int) {
        selectStart[0] = relativePage
        selectStart[1] = lineIndex
        selectStart[2] = charIndex
        val textLine = relativePage(relativePage).textLines[lineIndex]
        val textChar = textLine.textChars[charIndex]
        upSelectedStart(
            textChar.start,
            textLine.lineBottom + relativeOffset(relativePage),
            textLine.lineTop + relativeOffset(relativePage)
        )
        upSelectChars()
    }

    /**
     * 选择结束文字
     */
    fun selectEndMoveIndex(relativePage: Int, lineIndex: Int, charIndex: Int) {
        selectEnd[0] = relativePage
        selectEnd[1] = lineIndex
        selectEnd[2] = charIndex
        val textLine = relativePage(relativePage).textLines[lineIndex]
        val textChar = textLine.textChars[charIndex]
        upSelectedEnd(textChar.end, textLine.lineBottom + relativeOffset(relativePage))
        upSelectChars()
    }

    private fun upSelectChars() {
        //// 判断是滚动模式还是非滚动模式
        val last = if (callBack.isScroll) 2 else 0
        // 遍历每一页
        for (relativePos in 0..last) {
            // 遍历每一行
            for ((lineIndex, textLine) in relativePage(relativePos).textLines.withIndex()) {
                // 遍历每个字符
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    // 跳过段落开头的缩进空字符
                    if ((charIndex == 0||charIndex == 1) && textChar.charData == emptyString) continue

                    textChar.selected =
                        // 情况1：起始点和结束点在同一页、同一行，字符索引位于起止点之间
                        if (relativePos == selectStart[0] && relativePos == selectEnd[0] && lineIndex == selectStart[1] && lineIndex == selectEnd[1]) {
                            charIndex in selectStart[2]..selectEnd[2]
                        }
                        // 情况2：当前字符在起始点所在页的起始行内，且字符索引在起始点之后
                        else if (relativePos == selectStart[0] && lineIndex == selectStart[1]) {
                            charIndex >= selectStart[2]
                        }
                        // 情况3：当前字符在结束点所在页的结束行内，且字符索引在结束点之前
                        else if (relativePos == selectEnd[0] && lineIndex == selectEnd[1]) {
                            charIndex <= selectEnd[2]
                        }
                        // 情况4：起始点和结束点在同一页，但字符位于中间行
                        else if (relativePos == selectStart[0] && relativePos == selectEnd[0]) {
                            lineIndex in (selectStart[1] + 1) until selectEnd[1]
                        }
                        // 情况5：当前字符在起始点所在页，位于起始行之后的其他行
                        else if (relativePos == selectStart[0]) {
                            lineIndex > selectStart[1]
                        }
                        // 情况6：当前字符在结束点所在页，位于结束行之前的其他行
                        else if (relativePos == selectEnd[0]) {
                            lineIndex < selectEnd[1]
                        }
                        // 情况7：当前字符在完全选中范围内的中间页
                        else {
                            relativePos in selectStart[0] + 1 until selectEnd[0]
                        }
                }
            }
        }
        invalidate()
    }

    private fun upSelectedStart(x: Float, y: Float, top: Float) = callBack.apply {
        upSelectedStart(x, y + headerHeight, top + headerHeight)
    }

    private fun upSelectedEnd(x: Float, y: Float) = callBack.apply {
        upSelectedEnd(x, y + headerHeight)
    }

    fun cancelSelect() {
        val last = if (callBack.isScroll) 2 else 0
        for (relativePos in 0..last) {
            relativePage(relativePos).textLines.forEach { textLine ->
                textLine.textChars.forEach {
                    it.selected = false
                }
            }
        }
        invalidate()
        callBack.onCancelSelect()
    }

    val selectedText: String
        get() {
            val stringBuilder = StringBuilder()
            for (relativePos in selectStart[0]..selectEnd[0]) {
                val textPage = relativePage(relativePos)
                if (relativePos == selectStart[0] && relativePos == selectEnd[0]) {
                    for (lineIndex in selectStart[1]..selectEnd[1]) {
                        if (lineIndex == selectStart[1] && lineIndex == selectEnd[1]) {
                            stringBuilder.append(
                                textPage.textLines[lineIndex].text.substring(
                                    selectStart[2], selectEnd[2] + 1
                                )
                            )
                        } else if (lineIndex == selectStart[1]) {
                            stringBuilder.append(
                                textPage.textLines[lineIndex].text.substring(
                                    selectStart[2]
                                )
                            )
                        } else if (lineIndex == selectEnd[1]) {
                            stringBuilder.append(
                                textPage.textLines[lineIndex].text.substring(0, selectEnd[2] + 1)
                            )
                        } else {
                            stringBuilder.append(textPage.textLines[lineIndex].text)
                        }
                    }
                } else if (relativePos == selectStart[0]) {
                    for (lineIndex in selectStart[1] until relativePage(relativePos).textLines.size) {
                        if (lineIndex == selectStart[1]) {
                            stringBuilder.append(
                                textPage.textLines[lineIndex].text.substring(
                                    selectStart[2]
                                )
                            )
                        } else {
                            stringBuilder.append(textPage.textLines[lineIndex].text)
                        }
                    }
                } else if (relativePos == selectEnd[0]) {
                    for (lineIndex in 0..selectEnd[1]) {
                        if (lineIndex == selectEnd[1]) {
                            stringBuilder.append(
                                textPage.textLines[lineIndex].text.substring(0, selectEnd[2] + 1)
                            )
                        } else {
                            stringBuilder.append(textPage.textLines[lineIndex].text)
                        }
                    }
                } else if (relativePos in selectStart[0] + 1 until selectEnd[0]) {
                    for (lineIndex in selectStart[1]..selectEnd[1]) {
                        stringBuilder.append(textPage.textLines[lineIndex].text)
                    }
                }
            }
            return stringBuilder.toString()
        }

    private fun selectToInt(page: Int, line: Int, char: Int): Int {
        return page * 10000000 + line * 100000 + char
    }

    private fun selectToInt(select: Array<Int>): Int {
        return select[0] * 10000000 + select[1] * 100000 + select[2]
    }

    //相对位置
    private fun relativeOffset(relativePos: Int): Float {
        return when (relativePos) {
            0 -> pageOffset
            1 -> pageOffset + textPage.height
            else -> pageOffset + textPage.height + pageFactory.nextPage.height
        }
    }

    //相对页面
    private fun relativePage(relativePos: Int): TextPage {
        return when (relativePos) {
            0 -> textPage
            1 -> pageFactory.nextPage
            else -> pageFactory.nextPagePlus
        }
    }


    interface CallBack {
        fun upSelectedStart(x: Float, y: Float, top: Float)
        fun upSelectedEnd(x: Float, y: Float)
        fun onCancelSelect()
        val headerHeight: Int
        val pageFactory: TextPageFactory
        val scope: CoroutineScope
        val isScroll: Boolean
    }

}

/**
 * 线条类型枚举
 */
enum class LineType {
    STRAIGHT, // 直线
    DASHED,   // 虚线
    WAVY      // 波浪线
}
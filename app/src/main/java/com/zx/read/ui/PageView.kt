package com.zx.read.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.zx.read.DataSource
import com.zx.read.ReadBook
import com.zx.read.factory.TextPageFactory
import com.zx.read.bean.TextChapter
import com.zx.read.config.ReadBookConfig
import com.zx.read.delegate.CoverPageDelegate
import com.zx.read.delegate.NoAnimPageDelegate
import com.zx.read.delegate.PageDelegate
import com.zx.read.delegate.ScrollPageDelegate
import com.zx.read.delegate.SimulationPageDelegate
import com.zx.read.delegate.SlidePageDelegate
import com.zx.read.extensions.activity
import com.zx.read.extensions.screenshot
import com.zx.read.provider.ChapterProvider
import com.zx.readbook.R
import kotlin.math.abs


/**
 * @description:
 * @author: zhouxiang
 * @created: 2025/03/05 14:33
 * @version: V1.0
 */
class PageView(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs), DataSource {
    val callBack: CallBack get() = activity as CallBack
    var pageFactory: TextPageFactory = TextPageFactory(this)
    //翻页代理
    var pageDelegate: PageDelegate? = null
        private set(value) {
            field?.onDestroy()
            field = null
            field = value
            upContent()
        }
    //是否滚动
    var isScroll = false
    //上一页
    var prevPage: ContentView = ContentView(context)
    //当前页
    var curPage: ContentView = ContentView(context)
    //下一页
    var nextPage: ContentView = ContentView(context)

    val defaultAnimationSpeed = 300
    private var pressDown = false
    private var isMove = false
    //起始点
    var startX: Float = 0f
    var startY: Float = 0f

    //上一个触碰点
    var lastX: Float = 0f
    var lastY: Float = 0f

    //触碰点
    var touchX: Float = 0f
    var touchY: Float = 0f
    //是否停止动画动作
    var isAbortAnim = false
    //长按
    private var longPressed = false
    private val longPressTimeout = 600L
    private val longPressRunnable = Runnable {
        longPressed = true
        onLongPress()
    }
    var isTextSelected = false
    private var pressOnTextSelected = false
    private var firstRelativePage = 0
    private var firstLineIndex: Int = 0
    private var firstCharIndex: Int = 0

    val slopSquare by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    private val centerRectF = RectF(width * 0.33f, height * 0.33f, width * 0.66f, height * 0.66f)
    private val autoPageRect by lazy { Rect() }
    private val autoPagePint by lazy {
        Paint().apply {
            color =context.getColor(R.color.teal_200)
        }
    }

    init {
        addView(nextPage)
        addView(curPage)
        addView(prevPage)
        upBg()
        setWillNotDraw(false)
        upPageAnim()
    }
    fun upBg() {
        ReadBookConfig.bg ?: let {
            ReadBookConfig.upBg()
        }
        curPage.setBg(ReadBookConfig.bg)
        prevPage.setBg(ReadBookConfig.bg)
        nextPage.setBg(ReadBookConfig.bg)
    }
    fun upTipStyle() {
        curPage.upTipStyle()
        prevPage.upTipStyle()
        nextPage.upTipStyle()
    }

    fun upStyle() {
        ChapterProvider.upStyle()
        curPage.upStyle()
        prevPage.upStyle()
        nextPage.upStyle()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerRectF.set(width * 0.33f, height * 0.33f, width * 0.66f, height * 0.66f)
        prevPage.x = -w.toFloat()
        pageDelegate?.setViewSize(w, h)
        if (oldw != 0 && oldh != 0) {
            //重新加载内容
           ReadBook.loadContent(resetPageOffset = false)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        pageDelegate?.onDraw(canvas)
        if (!isInEditMode && callBack.isAutoPage && !isScroll) {
            nextPage.screenshot()?.let {
                val bottom = callBack.autoPageProgress
                autoPageRect.set(0, 0, width, bottom)
                canvas.drawBitmap(it, autoPageRect, autoPageRect, null)
                canvas.drawRect(
                    0f,
                    bottom.toFloat() - 1,
                    width.toFloat(),
                    bottom.toFloat(),
                    autoPagePint
                )
            }
        }
    }
    override fun computeScroll() {
        pageDelegate?.scroll()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }
    /**
     * 触摸事件
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        callBack.screenOffTimerStart()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isTextSelected) {
                    curPage.cancelSelect()
                    isTextSelected = false
                    pressOnTextSelected = true
                } else {
                    pressOnTextSelected = false
                }
                longPressed = false
                postDelayed(longPressRunnable, longPressTimeout)
                pressDown = true
                isMove = false
                pageDelegate?.onTouch(event)
                pageDelegate?.onDown()
                setStartPoint(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                pressDown = true
                if (!isMove) {
                    isMove =
                        abs(startX - event.x) > slopSquare || abs(startY - event.y) > slopSquare
                }
                if (isMove) {
                    longPressed = false
                    removeCallbacks(longPressRunnable)
                    if (isTextSelected) {
                        selectText(event.x, event.y)
                    } else {
                        pageDelegate?.onTouch(event)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                removeCallbacks(longPressRunnable)
                if (!pressDown) return true
                if (!isMove) {
                    if (!longPressed && !pressOnTextSelected) {
                        onSingleTapUp()
                        return true
                    }
                }
                if (isTextSelected) {
                    callBack.showTextActionMenu()
                } else if (isMove) {
                    pageDelegate?.onTouch(event)
                }
                pressOnTextSelected = false
            }
        }
        return true
    }

    /**
     * 保存开始位置
     */
    fun setStartPoint(x: Float, y: Float, invalidate: Boolean = true) {
        startX = x
        startY = y
        lastX = x
        lastY = y
        touchX = x
        touchY = y

        if (invalidate) {
            invalidate()
        }
    }

    /**
     * 保存当前位置
     */
    fun setTouchPoint(x: Float, y: Float, invalidate: Boolean = true) {
        lastX = touchX
        lastY = touchY
        touchX = x
        touchY = y
        if (invalidate) {
            invalidate()
        }
        pageDelegate?.onScroll()
    }
    /**
     * 长按选择
     */
    private fun onLongPress() {
        curPage.selectText(startX, startY) { relativePage, lineIndex, charIndex ->
            isTextSelected = true
            firstRelativePage = relativePage
            firstLineIndex = lineIndex
            firstCharIndex = charIndex
            curPage.selectStartMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
            curPage.selectEndMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
        }
    }
    /**
     * 单击
     */
    private fun onSingleTapUp(): Boolean {
        if (isTextSelected) {
            isTextSelected = false
            return true
        }
        if (centerRectF.contains(startX, startY)) {
            if (!isAbortAnim) {
                callBack.clickCenter()
            }
        } else if (ReadBookConfig.clickTurnPage) {
            if (startX > width / 2 || ReadBookConfig.clickAllNext) {
                pageDelegate?.nextPageByAnim(defaultAnimationSpeed)
            } else {
                pageDelegate?.prevPageByAnim(defaultAnimationSpeed)
            }
        }
        return true
    }
    /**
     * 选择文本
     */
    private fun selectText(x: Float, y: Float) {
        curPage.selectText(x, y) { relativePage, lineIndex, charIndex ->
            when {
                relativePage > firstRelativePage -> {
                    curPage.selectStartMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectEndMoveIndex(relativePage, lineIndex, charIndex)
                }
                relativePage < firstRelativePage -> {
                    curPage.selectEndMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectStartMoveIndex(relativePage, lineIndex, charIndex)
                }
                lineIndex > firstLineIndex -> {
                    curPage.selectStartMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectEndMoveIndex(relativePage, lineIndex, charIndex)
                }
                lineIndex < firstLineIndex -> {
                    curPage.selectEndMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectStartMoveIndex(relativePage, lineIndex, charIndex)
                }
                charIndex > firstCharIndex -> {
                    curPage.selectStartMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectEndMoveIndex(relativePage, lineIndex, charIndex)
                }
                else -> {
                    curPage.selectEndMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectStartMoveIndex(relativePage, lineIndex, charIndex)
                }
            }
        }
    }
    fun onDestroy() {
        pageDelegate?.onDestroy()
        curPage.cancelSelect()
    }
    fun fillPage(direction: PageDelegate.Direction) {
        when (direction) {
            PageDelegate.Direction.PREV -> {
                pageFactory.moveToPrev(true)
            }
            PageDelegate.Direction.NEXT -> {
                pageFactory.moveToNext(true)
            }
            else -> Unit
        }
    }
    fun upPageAnim() {
        isScroll = ReadBook.pageAnim() == 3
        when (ReadBook.pageAnim()) {
            0 -> if (pageDelegate !is CoverPageDelegate) {
                pageDelegate = CoverPageDelegate(this)
            }
            1 -> if (pageDelegate !is SlidePageDelegate) {
                pageDelegate = SlidePageDelegate(this)
            }
            2 -> if (pageDelegate !is SimulationPageDelegate) {
                pageDelegate = SimulationPageDelegate(this)
            }
            3 -> if (pageDelegate !is ScrollPageDelegate) {
                pageDelegate = ScrollPageDelegate(this)
            }
            else -> if (pageDelegate !is NoAnimPageDelegate) {
                pageDelegate = NoAnimPageDelegate(this)
            }
        }
    }

    /**
     * 更新内容
     */
    override fun upContent(relativePosition: Int, resetPageOffset: Boolean) {
        if (isScroll && !callBack.isAutoPage) {
            curPage.setContent(pageFactory.currentPage, resetPageOffset)
        } else {
            curPage.resetPageOffset()
            when (relativePosition) {
                -1 -> prevPage.setContent(pageFactory.prevPage)
                1 -> nextPage.setContent(pageFactory.nextPage)
                else -> {
                    curPage.setContent(pageFactory.currentPage)
                    nextPage.setContent(pageFactory.nextPage)
                    prevPage.setContent(pageFactory.prevPage)
                }
            }
        }
        callBack.screenOffTimerStart()
    }


    //当前章节内容
    override val currentChapter: TextChapter?
        get() {
            return if (callBack.isInitFinish) ReadBook.textChapter(0) else null
        }
    //下一章节内容
    override val nextChapter: TextChapter?
        get() {
            return if (callBack.isInitFinish) ReadBook.textChapter(1) else null
        }
    //上一章节内容
    override val prevChapter: TextChapter?
        get() {
            return if (callBack.isInitFinish) ReadBook.textChapter(-1) else null
        }
    //是否有下一章
    override fun hasNextChapter(): Boolean {
        return ReadBook.durChapterIndex < ReadBook.chapterSize - 1
    }

    //是否有上一章
    override fun hasPrevChapter(): Boolean {
        return ReadBook.durChapterIndex > 0
    }

    interface CallBack {
        val isInitFinish: Boolean
        val isAutoPage: Boolean
        val autoPageProgress: Int
        //点击中间回调
        fun clickCenter()
        fun screenOffTimerStart()
        //显示选择text菜单
        fun showTextActionMenu()
    }
}
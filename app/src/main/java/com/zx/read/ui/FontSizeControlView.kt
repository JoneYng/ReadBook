package com.zx.read.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.unit.dp
import com.zx.readbook.R
import kotlin.math.max
import kotlin.math.min


/**
 * @description:
 * @author: zhouxiang
 * @created: 2025/03/11 09:31
 * @version: V1.0
 */

class CustomSliderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // 自定义属性默认值
    private var valueMargin = 60f// 文字与滑道边缘的间距
    private var trackHeight = 120f // 轨道高度等于滑块直径
    private var thumbRadius = trackHeight/2-5// 滑块半径
    private var minValue = 0f
    private var maxValue = 100f
    private var currentValue = (minValue + maxValue) / 2
    private var thumbText = "边距"
    private var startText = "小"
    private var endText = "大"
    private var trackColor = Color.parseColor("#F8F8F8")
    private var progressColor = Color.parseColor("#ECECEE")
    private var thumbColor = Color.WHITE

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackColor
        style = Paint.Style.FILL
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        style = Paint.Style.FILL
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = thumbColor
        style = Paint.Style.FILL
        setShadowLayer(10f, 0f, 5f, Color.GRAY)
    }

    private val minTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 35f
        textAlign = Paint.Align.CENTER
    }

    private val maxTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private val thumbTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 35f
        textAlign = Paint.Align.CENTER
    }


    private var thumbX = 0f
    private var trackRect = RectF()

    var onValueChanged: ((Float) -> Unit)? = null

    init {
        initAttributes(context, attrs)
        setLayerType(LAYER_TYPE_SOFTWARE, null) // 让阴影生效
    }

    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.CustomSliderView).apply {
            try {
                trackHeight = getDimension(R.styleable.CustomSliderView_trackHeight, trackHeight)
                valueMargin = getFloat(R.styleable.CustomSliderView_valueMargin, valueMargin)
                minValue = getFloat(R.styleable.CustomSliderView_minValue, minValue)
                maxValue = getFloat(R.styleable.CustomSliderView_maxValue, maxValue)
                currentValue = getFloat(R.styleable.CustomSliderView_currentValue, currentValue)
                thumbText = getString(R.styleable.CustomSliderView_thumbText) ?: thumbText
                startText = getString(R.styleable.CustomSliderView_startText) ?: startText
                endText = getString(R.styleable.CustomSliderView_endText) ?: endText
                trackColor = getColor(R.styleable.CustomSliderView_trackColor, trackColor)
                progressColor = getColor(R.styleable.CustomSliderView_progressColor, progressColor)
                thumbColor = getColor(R.styleable.CustomSliderView_thumbColor, thumbColor)

                progressPaint.setColor(progressColor)
                trackPaint.setColor(trackColor)
                thumbPaint.setColor(thumbColor)
            } finally {
                recycle()
            }
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        trackRect.set(
            paddingLeft.toFloat(), height / 2f - trackHeight / 2,
            width - paddingRight.toFloat(), height / 2f + trackHeight / 2
        )
        updateThumbPosition()
    }
    fun setThumbText(text: String) {
        thumbText = text
        invalidate() // 触发重绘
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 画轨道
        canvas.drawRoundRect(trackRect, trackHeight / 2, trackHeight / 2, trackPaint)
        // 画已滑动的进度条
        val progressRect =
            RectF(trackRect.left, trackRect.top, thumbX + thumbRadius, trackRect.bottom)
        canvas.drawRoundRect(progressRect, trackHeight / 2, trackHeight / 2, progressPaint)

        val minTextBounds = Rect()
        minTextPaint.getTextBounds(startText, 0, 1, minTextBounds)
        // 画两端的“小”和“大”文字，使其位于滑道内部
        canvas.drawText(
            startText,
            trackRect.left + valueMargin,
            height / 2f + minTextBounds.height() / 2,
            minTextPaint
        )

        val maxTextBounds = Rect()
        maxTextPaint.getTextBounds(startText, 0, 1, maxTextBounds)
        canvas.drawText(
            endText,
            trackRect.right - valueMargin,
            height / 2f + maxTextBounds.height() / 2,
            maxTextPaint
        )

        // 画滑块
        canvas.drawCircle(thumbX, height / 2f, thumbRadius, thumbPaint)

        val thumbTextBounds = Rect()
        thumbTextPaint.getTextBounds(startText, 0, 1, thumbTextBounds)
        val textY = height / 2f + thumbTextBounds.height() / 2
        canvas.drawText(thumbText, thumbX, textY, thumbTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateValueByTouch(event.x)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateValueByTouch(x: Float) {
        // 限制滑块移动范围，避免滑块超出轨道
        val clampedX = max(trackRect.left + thumbRadius, min(trackRect.right - thumbRadius, x))
        val fraction =
            (clampedX - trackRect.left - thumbRadius) / (trackRect.width() - 2 * thumbRadius)
        currentValue = minValue + fraction * (maxValue - minValue)
        thumbX = clampedX
        onValueChanged?.invoke(currentValue) // 通知外部监听
        invalidate()
    }

    private fun updateThumbPosition() {
        val fraction = (currentValue - minValue) / (maxValue - minValue)
        thumbX = trackRect.left + thumbRadius + fraction * (trackRect.width() - 2 * thumbRadius)
    }

    fun setValue(value: Float) {
        currentValue = max(minValue, min(maxValue, value))
        updateThumbPosition()
        invalidate()
    }

    fun getValue(): Float = currentValue
}
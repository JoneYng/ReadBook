package com.zx.read.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isGone
import com.zx.read.bean.TextPage
import com.zx.read.config.ReadBookConfig
import com.zx.read.config.ReadTipConfig
import com.zx.read.extensions.dp
import com.zx.read.extensions.statusBarHeight
import com.zx.read.extensions.visible
import com.zx.read.provider.ChapterProvider
import com.zx.readbook.R
import com.zx.readbook.databinding.ViewBookPageBinding


/**
 * @description:
 * @author: zhouxiang
 * @created: 2025/03/05 14:39
 * @version: V1.0
 */
class ContentView(context: Context) : FrameLayout(context) {
    private val binding = ViewBookPageBinding.inflate(LayoutInflater.from(context), this, true)
    private var battery = 100
    private var tvPageAndTotal: BatteryView? = null

    val headerHeight: Int
        get() {
            val h1 = if (ReadBookConfig.hideStatusBar) 0 else context.statusBarHeight
            val h2 = 0
            return h1 + h2
        }

    init {
        //设置背景防止切换背景时文字重叠
        setBackgroundColor(context.getCompatColor(R.color.background))
        upTipStyle()
        upStyle()
        binding.contentTextView.upView = {
            setProgress(it)
        }
    }

    fun upStyle() = with(binding){
        ReadBookConfig.apply {
            tvFooterRight.typeface = ChapterProvider.typeface
            tvFooterRight.setColor(textColor)
            upStatusBar()
            vwTopDivider.visible(showHeaderLine)
            vwBottomDivider.visible(showFooterLine)
            binding.contentTextView.upVisibleRect()
        }
    }

    /**
     * 显示状态栏时隐藏header
     */
    fun upStatusBar() = with(binding.vwStatusBar){
        setPadding(paddingLeft, context.statusBarHeight, paddingRight, paddingBottom)
        isGone =
            ReadBookConfig.hideStatusBar
    }

    fun upTipStyle()  = with(binding) {
        ReadTipConfig.apply {
            tvFooterRight.isGone = tipFooterRight == none
            llFooter.isGone = hideFooter
        }
        tvPageAndTotal = when (ReadTipConfig.pageAndTotal) {
            ReadTipConfig.tipFooterRight -> tvFooterRight
            else -> null
        }
        tvPageAndTotal?.apply {
            isBattery = false
            textSize = 12f
        }
    }

    fun setBg(bg: Drawable?) {
        binding.pagePanel.background = bg
    }


    fun setContent(textPage: TextPage, resetPageOffset: Boolean = true) {
        setProgress(textPage)
        if (resetPageOffset)
            resetPageOffset()
        binding.contentTextView.setContent(textPage)
    }

    fun resetPageOffset() {
        binding.contentTextView.resetPageOffset()
    }

    @SuppressLint("SetTextI18n")
    fun setProgress(textPage: TextPage) = textPage.apply {
        tvPageAndTotal?.text = "${index.plus(1)}/$pageSize  $readProgress"
    }

    fun onScroll(offset: Float) {
        binding.contentTextView.onScroll(offset)
    }

    fun upSelectAble(selectAble: Boolean) {
        binding.contentTextView.selectAble = selectAble
    }

    fun selectText(
        x: Float, y: Float,
        select: (relativePage: Int, lineIndex: Int, charIndex: Int) -> Unit
    ) {
        return binding.contentTextView.selectText(x, y - headerHeight, select)
    }

    fun selectStartMove(x: Float, y: Float,startCursorOutOfBounds: () -> Unit) {
        binding.contentTextView.selectStartMove(x, y - headerHeight,startCursorOutOfBounds)
    }

    fun selectStartMoveIndex(relativePage: Int, lineIndex: Int, charIndex: Int) {
        binding.contentTextView.selectStartMoveIndex(relativePage, lineIndex, charIndex)
    }

    fun selectEndMove(x: Float, y: Float,endCursorOutOfBounds:()->Unit) {
        binding.contentTextView.selectEndMove(x, y - headerHeight,endCursorOutOfBounds)
    }

    fun selectEndMoveIndex(relativePage: Int, lineIndex: Int, charIndex: Int) {
        binding.contentTextView.selectEndMoveIndex(relativePage, lineIndex, charIndex)
    }

    fun cancelSelect() {
        binding.contentTextView.cancelSelect()
    }

    val selectedText: String get() = binding.contentTextView.selectedText

}
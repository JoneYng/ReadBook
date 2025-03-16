package com.zx.read.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import com.zx.read.bean.TextChar
import com.zx.read.bean.TextPage
import com.zx.read.config.ReadBookConfig
import com.zx.read.extensions.statusBarHeight
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
    // 创建一个按钮并设置样式
    val button = Button(context).apply {
        text = "去考试"
        setBackgroundColor(Color.BLUE)
        setOnClickListener {
            Toast.makeText(context, "去考试", Toast.LENGTH_SHORT).show()
        }
    }

    init {
        //设置背景防止切换背景时文字重叠
        setBackgroundColor(context.getCompatColor(R.color.background))
        upTipStyle()
        upStyle()
        binding.contentTextView.upView = {
            setProgress(it)
        }

        binding.contentTextView.drawLayout = { top->
            if(button.parent!=null){
                if(top>0){
                    val layoutParams = button.layoutParams as ViewGroup.MarginLayoutParams
                    val marginTop = layoutParams.topMargin ?: 0
                    if(top.toInt()!=marginTop){
                        layoutParams.setMargins(50, top.toInt(), 50, 0)
                        button.layoutParams = layoutParams
                        button.visibility=View.VISIBLE
                    }
                }else{
                    button.visibility=View.GONE
                }
            }
        }
    }

    fun upStyle() = with(binding){
        ReadBookConfig.apply {
            tvFooterRight.typeface = ChapterProvider.typeface
            tvFooterRight.setColor(textColor)
            upStatusBar()
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
        tvPageAndTotal =tvFooterRight
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

    }

    fun resetPageOffset() {
        binding.contentTextView.resetPageOffset()
    }



    @SuppressLint("SetTextI18n")
    fun setProgress(textPage: TextPage) = textPage.apply {
        Log.i("drawLayout", "setProgress:$textPage")

        //设置当前布局
        binding.flBookPage.removeView(button)
        binding.contentTextView.setContent(textPage)
        textPage.textLines.forEach {
            if(it.isLayout){
                // 创建 LayoutParams 并设置位置
                val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, // 宽度
                    FrameLayout.LayoutParams.WRAP_CONTENT  // 高度
                ).apply {
                    gravity = Gravity.TOP or Gravity.END  // 设置控件在右上角
                    setMargins(50, it.lineTop.toInt(), 50, 0)             // 设置具体的偏移位置
                }
                // 将按钮添加到 FrameLayout
                binding.flBookPage.addView(button, layoutParams)
            }
        }

        //设置当前页码，进度
        tvPageAndTotal?.text = "${index.plus(1)}/$pageSize  $readProgress"
    }

    fun onScroll(offset: Float) {
        binding.contentTextView.onScroll(offset)
    }

    fun upSelectAble(selectAble: Boolean) {
        binding.contentTextView.selectAble = selectAble
    }


    fun  setTextUnderline(type: LineType){
        binding.contentTextView.setTextUnderline(type)
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

    fun contentTextView():ContentTextView {
       return binding.contentTextView
    }

    val selectedText: ArrayList<TextChar> get() = binding.contentTextView.selectedText

}
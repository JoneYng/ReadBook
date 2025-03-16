package com.zx.read.bean

import com.zx.read.ui.LineType


/**
 * 每一个字符
 */
data class TextChar(
    val charData: String,//内容
    var start: Float,//开始位置
    var end: Float,//结束位置
    var type: LineType? = null,//划线类型
    var isUnderLineStart: Boolean = false,//是否开始
    var isUnderLineEnd: Boolean = false,//是否结束
    var selected: Boolean = false,//是否选中
    var isImage: Boolean = false//是否是图片
)

/***
 * 划线
 */
data class UnderLine(
    var startX: Float = 0f,
    var endX: Float = 0f,
    var selectedText: String = "",
    var type: LineType,//划线类型
)
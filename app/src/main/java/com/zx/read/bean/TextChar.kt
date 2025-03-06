package com.zx.read.bean


/**
 * 每一个字符
 */
data class TextChar(
    val charData: String,//内容
    var start: Float,//开始位置
    var end: Float,//结束位置
    var selected: Boolean = false,//是否选中
    var isImage: Boolean = false//是否是图片
)
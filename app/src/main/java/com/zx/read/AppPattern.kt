package com.zx.read

import java.util.regex.Pattern

object AppPattern {
    //图片正则
    val imgPattern: Pattern =
        Pattern.compile("<img .*?src.*?=.*?\"(.*?(?:,\\{.*\\})?)\".*?>", Pattern.CASE_INSENSITIVE)
    //布局正则
    val layoutPattern: Pattern =
        Pattern.compile("<layout .*?type.*?=.*?\"(.*?(?:,\\{.*\\})?)\".*?>", Pattern.CASE_INSENSITIVE)
    val fileNameRegex = Regex("[\\\\/:*?\"<>|.]")
}
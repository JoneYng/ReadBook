package com.zx.read

import java.util.regex.Pattern

object AppPattern {
    val imgPattern: Pattern =
        Pattern.compile("<img .*?src.*?=.*?\"(.*?(?:,\\{.*\\})?)\".*?>", Pattern.CASE_INSENSITIVE)
    val fileNameRegex = Regex("[\\\\/:*?\"<>|.]")
}
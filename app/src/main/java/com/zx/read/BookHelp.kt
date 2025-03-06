package com.zx.read

import android.util.Log

import com.zx.read.bean.Book
import com.zx.read.config.ReadBookConfig
import com.zx.read.extensions.externalFilesDir
import com.zx.read.utils.FileUtils
import com.zx.read.utils.MD5Utils

import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

object BookHelp {
    private const val cacheFolderName = "book_cache"
    private const val cacheImageFolderName = "images"
    private val downloadDir: File = App.INSTANCE.externalFilesDir
    private val downloadImages = CopyOnWriteArraySet<String>()
//    private val imageRepository by lazy { ImageRepository() }

    suspend fun saveImage(book: Book, src: String) {
        while (downloadImages.contains(src)) {
            delay(100)
        }
        if (getImage(book, src).exists()) {
            return
        }
        Log.e("saveImage", "saveImage: $src", )
        downloadImages.add(src)
        try {
//            imageRepository.getImage(src).let {
//                val c=it
//                Log.e("saveImage", "bytes: $c", )
//                FileUtils.createFileIfNotExist(
//                    downloadDir,
//                    cacheFolderName,
//                    book.getFolderName(),
//                    cacheImageFolderName,
//                    "${MD5Utils.md5Encode16(src)}${getImageSuffix(src)}"
//                ).writeBytes(c.bytes())
//            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            downloadImages.remove(src)
        }
    }

    fun getImage(book: Book, src: String): File {
        return FileUtils.getFile(
            downloadDir,
            cacheFolderName,
            book.getFolderName(),
            cacheImageFolderName,
            "${MD5Utils.md5Encode16(src)}${getImageSuffix(src)}"
        )
    }

    private fun getImageSuffix(src: String): String {
        var suffix = src.substringAfterLast(".").substringBefore(",")
        if (suffix.length > 5) {
            suffix = ".jpg"
        }
        return suffix
    }


    suspend fun disposeContent(
        book: Book,
        title: String,
        content: String
    ): List<String> {
        var title1 = title
        var content1 = content
        try {
//            when (AppConfig.chineseConverterType) {
//                1 -> {
//                    title1=ZHConverter.getInstance(ZHConverter.SIMPLIFIED).convert(title1)
//                    content1=ZHConverter.getInstance(ZHConverter.SIMPLIFIED).convert(content1)
//                }
//                2 -> {
//                    title1=ZHConverter.getInstance(ZHConverter.TRADITIONAL).convert(title1)
//                    content1=ZHConverter.getInstance(ZHConverter.TRADITIONAL).convert(content1)
//                }
//            }
        } catch (e: Exception) {
            withContext(Main) {
                App.INSTANCE.toast("简繁转换出错")
            }
        }
        val contents = arrayListOf<String>()
        content1.split("\n").forEach {
            //去除字符串开头的所有换行符（\n）、空格（ ）和回车符（\r）
            val str = it.replace("^[\\n\\s\\r]+".toRegex(), "")
            if (contents.isEmpty()) {
                contents.add(title1)
                if (str != title1 && str.isNotEmpty()) {
                    contents.add("${ReadBookConfig.paragraphIndent}$str")
                }
            } else if (str.isNotEmpty()) {
                contents.add("${ReadBookConfig.paragraphIndent}$str")
            }
        }
        return contents
    }
}
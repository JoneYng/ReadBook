package com.zx.read

import androidx.lifecycle.MutableLiveData
import com.zx.read.bean.Book
import com.zx.read.bean.BookChapter
import com.zx.read.bean.TextChapter
import com.zx.read.config.ReadBookConfig
import com.zx.read.coroutine.Coroutine
import com.zx.read.provider.ChapterProvider
import com.zx.read.provider.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.getStackTraceString
import org.jetbrains.anko.toast

object ReadBook {
    var book: Book? = null
    var chapterSize = 0

    //当前章节位置
    var durChapterIndex = 0

    //当前页面位置
    var durPageIndex = 0
    var callBack: CallBack? = null

    //上一章节
    var prevTextChapter: TextChapter? = null

    //当前章节
    var curTextChapter: TextChapter? = null

    //下一章节
    var nextTextChapter: TextChapter? = null
    var msg: String? = null
    private val loadingChapters = arrayListOf<Int>()

    fun resetData(book: Book) {
        this.book = book
        durChapterIndex = book.durChapterIndex
        durPageIndex = book.durChapterPos
        chapterSize = 1
        prevTextChapter = null
        curTextChapter = null
        nextTextChapter = null
        ImageProvider.clearAllCache()
        synchronized(this) {
            loadingChapters.clear()
        }
    }

    fun pageAnim(): Int {
        return ReadBookConfig.pageAnim
    }

    fun durChapterPos(): Int {
        curTextChapter?.let {
            if (durPageIndex < it.pageSize) {
                return durPageIndex
            }
            return it.pageSize - 1
        }
        return durPageIndex
    }

    /**
     * chapterOnDur: 0为当前页,1为下一页,-1为上一页
     */
    fun textChapter(chapterOnDur: Int = 0): TextChapter? {
        return when (chapterOnDur) {
            0 -> curTextChapter
            1 -> nextTextChapter
            -1 -> prevTextChapter
            else -> null
        }
    }


    private fun addLoading(index: Int): Boolean {
        synchronized(this) {
            if (loadingChapters.contains(index)) return false
            loadingChapters.add(index)
            return true
        }
    }

    fun removeLoading(index: Int) {
        synchronized(this) {
            loadingChapters.remove(index)
        }
    }

    fun skipToPage(page: Int) {
        durPageIndex = page
        callBack?.upContent()
        curPageChanged()
        saveRead()
    }

    fun setPageIndex(pageIndex: Int) {
        durPageIndex = pageIndex
        saveRead()
        curPageChanged()
    }

    private fun curPageChanged() {
        callBack?.pageChanged()
    }

    fun saveRead() {

    }

    /**
     * 移至下一章
     */
    fun moveToNextChapter(upContent: Boolean): Boolean {
        if (durChapterIndex < chapterSize - 1) {
            durPageIndex = 0
            durChapterIndex++
            prevTextChapter = curTextChapter
            curTextChapter = nextTextChapter
            nextTextChapter = null
            book?.let {
                if (curTextChapter == null) {
                    loadContent(durChapterIndex, upContent, false)
                } else if (upContent) {
                    callBack?.upContent()
                }
                loadContent(durChapterIndex.plus(1), upContent, false)
                GlobalScope.launch(Dispatchers.IO) {
                    for (i in 2..9) {
                        delay(1000)
                        download(durChapterIndex + i)
                    }
                }
            }
            saveRead()
            callBack?.upView()
            curPageChanged()
            return true
        } else {
            return false
        }
    }

    /**
     * 移至上一章
     */
    fun moveToPrevChapter(upContent: Boolean, toLast: Boolean = true): Boolean {
        if (durChapterIndex > 0) {
            durPageIndex = if (toLast) prevTextChapter?.lastIndex ?: 0 else 0
            durChapterIndex--
            nextTextChapter = curTextChapter
            curTextChapter = prevTextChapter
            prevTextChapter = null
            book?.let {
                if (curTextChapter == null) {
                    loadContent(durChapterIndex, upContent, false)
                } else if (upContent) {
                    callBack?.upContent()
                }
                loadContent(durChapterIndex.minus(1), upContent, false)
                GlobalScope.launch(Dispatchers.IO) {
                    for (i in 2..9) {
                        delay(1000)
                        download(durChapterIndex + i)
                    }
                }
            }
            saveRead()
            callBack?.upView()
            curPageChanged()
            return true
        } else {
            return false
        }
    }

    /**
     * 加载章节内容
     */
    fun loadContent(resetPageOffset: Boolean) {
        loadContent(durChapterIndex, resetPageOffset = resetPageOffset)
        loadContent(durChapterIndex + 1, resetPageOffset = resetPageOffset)
        loadContent(durChapterIndex - 1, resetPageOffset = resetPageOffset)
    }

    /**
     * @param index 当前章节位置
     * @param upContent
     * @param resetPageOffset
     */
    fun loadContent(index: Int, upContent: Boolean = true, resetPageOffset: Boolean) {
//        book?.let { book ->
//
//        }
        if (addLoading(index)) {
            Coroutine.async {
                val book = Book(
                    authorPenname = "作者",
                    bookId = 1,
                    bookName = "政治经济学",
                    bookStatus = "",
                    categoryName = "类别名称",
                    channelName = "频道名称",
                    cName = "政治经济学C",
                    coverImageUrl = "https://png.pngtree.com/thumb_back/fh260/background/20210331/pngtree-beautiful-cartoon-cloud-color-mobile-phone-wallpaper-image_598219.jpg",
                    introduction = "简介简介",
                    keyWord = "keyWord",
                    lastUpdateChapterDate = "2025-03-01",
                    status = 0,
                    wordCount = 0
                )
                //重置当前书
                ReadBook.resetData(book)
                val chapter = BookChapter(
                    chapterId = 11,
                    bookId = 1,
                    chapterIndex = 0,
                    chapterName = "第一篇 导论",
                    createTimeValue = 0,
                    updateDate = "",
                    updateTimeValue = 0,
                    chapterUrl = ""
                )
                var it =
                    "资本主义的发展及内在矛盾的尖锐化马克思主义产生于 19世纪 40每代。马克思、恩格斯生活的时代，" + "贫不主义生广万式住四 欧已经有了相当的发展，资本主义一方面带来了社会化大生产的迅猛发展，" + "另一方面又造成了 深重的社会灾难：一是社会两极分化，二是周期性经济危机频繁爆发。资本主义" + "内在的、固有 的矛盾已经暴露出来，预示着未来社会革命的性质和历史发展的方向\n"+
                    "资本主义的发展及内在矛盾的尖锐化马克思主义产生于 19世纪 40每代。马克思、恩格斯生活的时代，" + "贫不主义生广万式住四 欧已经有了相当的发展，资本主义一方面带来了社会化大生产的迅猛发展，" + "另一方面又造成了 深重的社会灾难：一是社会两极分化，二是周期性经济危机频繁爆发。资本主义" + "内在的、固有 的矛盾已经暴露出来，预示着未来社会革命的性质和历史发展的方向\n"+
                    "资本主义的发展及内在矛盾的尖锐化马克思主义产生于 19世纪 40每代。马克思、恩格斯生活的时代，" + "贫不主义生广万式住四 欧已经有了相当的发展，资本主义一方面带来了社会化大生产的迅猛发展，" + "另一方面又造成了 深重的社会灾难：一是社会两极分化，二是周期性经济危机频繁爆发。资本主义" + "内在的、固有 的矛盾已经暴露出来，预示着未来社会革命的性质和历史发展的方向\n"+
                    "资本主义的发展及内在矛盾的尖锐化马克思主义产生于 19世纪 40每代。马克思、恩格斯生活的时代，" + "贫不主义生广万式住四 欧已经有了相当的发展，资本主义一方面带来了社会化大生产的迅猛发展，" + "另一方面又造成了 深重的社会灾难：一是社会两极分化，二是周期性经济危机频繁爆发。资本主义" + "内在的、固有 的矛盾已经暴露出来，预示着未来社会革命的性质和历史发展的方向\n"+
                    "资本主义的发展及内在矛盾的尖锐化马克思主义产生于 19世纪 40每代。马克思、恩格斯生活的时代，" + "贫不主义生广万式住四 欧已经有了相当的发展，资本主义一方面带来了社会化大生产的迅猛发展，" + "另一方面又造成了 深重的社会灾难：一是社会两极分化，二是周期性经济危机频繁爆发。资本主义" + "内在的、固有 的矛盾已经暴露出来，预示着未来社会革命的性质和历史发展的方向\n"+
                    "资本主义的发展及内在矛盾的尖锐化马克思主义产生于 19世纪 40每代。马克思、恩格斯生活的时代，" + "贫不主义生广万式住四 欧已经有了相当的发展，资本主义一方面带来了社会化大生产的迅猛发展，" + "另一方面又造成了 深重的社会灾难：一是社会两极分化，二是周期性经济危机频繁爆发。资本主义" + "内在的、固有 的矛盾已经暴露出来，预示着未来社会革命的性质和历史发展的方向\n"+
                    "资本主义的发展及内在矛盾的尖锐化马克思主义产生于 19世纪 40每代。马克思、恩格斯生活的时代，" + "贫不主义生广万式住四 欧已经有了相当的发展，资本主义一方面带来了社会化大生产的迅猛发展，" + "另一方面又造成了 深重的社会灾难：一是社会两极分化，二是周期性经济危机频繁爆发。资本主义" + "内在的、固有 的矛盾已经暴露出来，预示着未来社会革命的性质和历史发展的方向\n"+
                    "资本主义的发展及内在矛盾的尖锐化马克思主义产生于 19世纪 40每代。马克思、恩格斯生活的时代，" + "贫不主义生广万式住四 欧已经有了相当的发展，资本主义一方面带来了社会化大生产的迅猛发展，" + "另一方面又造成了 深重的社会灾难：一是社会两极分化，二是周期性经济危机频繁爆发。资本主义" + "内在的、固有 的矛盾已经暴露出来，预示着未来社会革命的性质和历史发展的方向\n"
//                    //处理内容
                contentLoadFinish(book, chapter, it, upContent, resetPageOffset)
                removeLoading(chapter.chapterIndex)
            }.onError {
                removeLoading(index)
            }
        }
    }

    /**
     * 内容加载完成
     */
    fun contentLoadFinish(
        book: Book,
        chapter: BookChapter,
        content: String,
        upContent: Boolean = true,
        resetPageOffset: Boolean
    ) {
        Coroutine.async {
            if (chapter.chapterIndex in durChapterIndex - 1..durChapterIndex + 1) {
//                chapter.chapterName = when (AppConfig.chineseConverterType) {
//                    1 -> ZHConverter.getInstance(ZHConverter.SIMPLIFIED).convert(chapter.chapterName)
//                    2 -> ZHConverter.getInstance(ZHConverter.TRADITIONAL).convert(chapter.chapterName)
//                    else -> chapter.chapterName
//                }
                val contents = BookHelp.disposeContent(
                    book, chapter.chapterName, content
                )
                when (chapter.chapterIndex) {
                    //当前章节位置
                    durChapterIndex -> {
                        //当前章节
                        curTextChapter =
                            ChapterProvider.getTextChapter(book, chapter, contents, chapterSize, "")
                        if (upContent) callBack?.upContent(resetPageOffset = resetPageOffset)
                        callBack?.upView()
                        curPageChanged()
                        callBack?.contentLoadFinish()
                        ImageProvider.clearOut(durChapterIndex)
                    }

                    durChapterIndex - 1 -> {
                        prevTextChapter =
                            ChapterProvider.getTextChapter(book, chapter, contents, chapterSize, "")
                        if (upContent) callBack?.upContent(-1, resetPageOffset)
                    }

                    durChapterIndex + 1 -> {
                        nextTextChapter =
                            ChapterProvider.getTextChapter(book, chapter, contents, chapterSize, "")
                        if (upContent) callBack?.upContent(1, resetPageOffset)
                    }
                }
            }
        }.onError {
            it.printStackTrace()
            App.INSTANCE.toast("ChapterProvider ERROR:\n${it.getStackTraceString()}")
        }
    }

    private fun download(index: Int) {

    }

    interface CallBack {
        //更新目录
        fun loadChapterList(book: Book)

        //更新内容
        fun upContent(relativePosition: Int = 0, resetPageOffset: Boolean = true)
        fun upView()
        fun pageChanged()
        fun contentLoadFinish()
        fun upPageAnim()
    }

}
package com.zx.read

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import com.zx.read.bean.Book
import com.zx.read.dialog.ReadStyleDialog
import com.zx.read.dialog.TextActionMenu
import com.zx.read.extensions.invisible
import com.zx.read.extensions.statusBarHeight
import com.zx.read.extensions.visible
import com.zx.read.factory.TextPageFactory
import com.zx.read.ui.ContentTextView
import com.zx.read.ui.PageView
import com.zx.read.ui.ReadMenu
import com.zx.readbook.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast

class ReadActivity : AppCompatActivity(), View.OnTouchListener, PageView.CallBack,
    TextActionMenu.CallBack, ContentTextView.CallBack, ReadBook.CallBack, ReadMenu.CallBack,
    CoroutineScope by MainScope() {
    lateinit var pageView: PageView
    lateinit var readMenu: ReadMenu
    lateinit var textMenuPosition: View
    lateinit var cursorLeft: ImageView
    lateinit var cursorRight: ImageView
    override val headerHeight: Int get() = pageView.curPage.headerHeight
    override val pageFactory: TextPageFactory get() = pageView.pageFactory
    override val scope: CoroutineScope get() = this
    override val isScroll: Boolean get() = pageView.isScroll
    override val isInitFinish: Boolean get() = true
    override val isAutoPage: Boolean get() = false
    override val autoPageProgress: Int get() = 0
    override val selectedText: String
        get() = pageView.curPage.selectedText


    override fun onMenuActionFinally() {
    }

    private val textActionMenu: TextActionMenu by lazy {
        TextActionMenu(this, this)
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                textActionMenu.dismiss()
            }
            MotionEvent.ACTION_MOVE -> {
                handleCursorSelection(event, cursorLeft, cursorRight,v, pageView)
            }

            MotionEvent.ACTION_UP -> showTextActionMenu()
        }
        return true
    }

    override fun upSelectedStart(x: Float, y: Float, top: Float) {
        cursorLeft.visible(true)
        cursorLeft.x = x - cursorLeft.width
        cursorLeft.y = y
        textMenuPosition.x = x
        textMenuPosition.y = top
    }

    override fun upSelectedEnd(x: Float, y: Float) {
        cursorRight.x = x
        cursorRight.y = y
        cursorRight.visible(true)
    }

    override fun onCancelSelect() {
        cursorLeft.invisible()
        cursorRight.invisible()
        textActionMenu.dismiss()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)
        pageView = findViewById<PageView>(R.id.page_view)
        textMenuPosition = findViewById<View>(R.id.text_menu_position)
        cursorLeft = findViewById<ImageView>(R.id.cursor_left)
        cursorRight = findViewById<ImageView>(R.id.cursor_right)
        readMenu = findViewById<ReadMenu>(R.id.read_menu)
        ReadBook.callBack = this
        initView()
        ReadBook.loadContent(resetPageOffset = true)
    }

    /**
     * 初始化View
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        cursorLeft.setColorFilter(getColor(R.color.purple_200))
        cursorRight.setColorFilter(getColor(R.color.purple_200))
        cursorLeft.setOnTouchListener(this)
        cursorRight.setOnTouchListener(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ReadBook.loadContent(resetPageOffset = false)
    }

    override fun clickCenter() {
        readMenu.runMenuIn()
    }

    override fun screenOffTimerStart() {
    }

    override fun showTextActionMenu() {
        textActionMenu.let { popup ->
            popup.contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val popupHeight = popup.contentView.measuredHeight
            val x = textMenuPosition.x.toInt()
            var y = textMenuPosition.y.toInt() - popupHeight/2
            if (y < statusBarHeight) {
                y = (cursorLeft.y + cursorLeft.height).toInt()
            }
            if (cursorRight.y > y && cursorRight.y < y + popupHeight) {
                y = (cursorRight.y + cursorRight.height).toInt()
            }
            if (!popup.isShowing) {
                popup.showAtLocation(textMenuPosition, Gravity.TOP or Gravity.START, x, y)
            } else {
                popup.update(
                    x,
                    y,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

    }

    override fun loadChapterList(book: Book) {
    }

    override fun upContent(relativePosition: Int, resetPageOffset: Boolean) {
        launch {
            pageView.upContent(relativePosition, resetPageOffset)
            readMenu.setSeekPage(ReadBook.durPageIndex)
        }
    }

    override fun upView() {
        readMenu.upBookView()
    }

    override fun pageChanged() {
        readMenu.setSeekPage(ReadBook.durPageIndex)
    }

    override fun contentLoadFinish() {

    }

    override fun upPageAnim() {
        launch {
            pageView.upPageAnim()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pageView.onDestroy()
        ReadBook.msg = null
    }


    override fun openChapterList() {
//        ReadBook.book?.let {
//            startActivityForResult<ChapterListActivity>(
//                requestCodeChapterList,
//                Pair("bookId", it.bookId.toString())
//            )
//        }
    }

    override fun showAdjust() {
    }

    override fun showReadStyle() {
        ReadStyleDialog {
            pageView.upBg()
            pageView.upTipStyle()
            pageView.upStyle()
            ReadBook.loadContent(resetPageOffset = false)
        }.show(supportFragmentManager, "readStyle")
    }


}
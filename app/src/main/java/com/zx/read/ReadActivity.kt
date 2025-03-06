package com.zx.read

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import com.zx.read.bean.Book
import com.zx.read.extensions.invisible
import com.zx.read.extensions.visible
import com.zx.read.factory.TextPageFactory
import com.zx.read.ui.ContentTextView
import com.zx.read.ui.PageView
import com.zx.readbook.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast

class ReadActivity : AppCompatActivity(),
    View.OnTouchListener, PageView.CallBack, ContentTextView.CallBack,
    ReadBook.CallBack,
    CoroutineScope by MainScope() {
    lateinit var pageView: PageView
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
//                textActionMenu.dismiss()
            }

            MotionEvent.ACTION_MOVE -> {
                when (v.id) {
                    R.id.cursor_left -> pageView.curPage.selectStartMove(
                        event.rawX + cursorLeft.width,
                        event.rawY - cursorLeft.height
                    )
                    R.id.cursor_right -> pageView.curPage.selectEndMove(
                        event.rawX - cursorRight.width,
                        event.rawY - cursorRight.height
                    )
                }
            }

            MotionEvent.ACTION_UP -> showTextActionMenu()
        }
        return true
    }

    override fun upSelectedStart(x: Float, y: Float, top: Float) {
        cursorLeft.visible(true)
        cursorLeft.x = x - cursorLeft.width
        cursorLeft.y = y
        Log.i("zxzx","upSelectedStart:"+cursorLeft.x+"==="+cursorLeft.y )
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
//        textActionMenu.dismiss()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)
        pageView =findViewById<PageView>(R.id.page_view)
        textMenuPosition = findViewById<View>(R.id.text_menu_position)
        cursorLeft = findViewById<ImageView>(R.id.cursor_left)
        cursorRight = findViewById<ImageView>(R.id.cursor_right)
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
        App.INSTANCE.toast("点击中间")
    }

    override fun screenOffTimerStart() {
    }

    override fun showTextActionMenu() {
    }

    override fun loadChapterList(book: Book) {
    }

    override fun upContent(relativePosition: Int, resetPageOffset: Boolean) {
        launch {
            pageView.upContent(relativePosition, resetPageOffset)

        }
    }
    override fun upView() {

    }
    override fun pageChanged() {
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

}
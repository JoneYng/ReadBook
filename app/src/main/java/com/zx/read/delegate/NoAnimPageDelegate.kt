package com.zx.read.delegate

import android.graphics.Canvas
import com.zx.read.ui.PageView

class NoAnimPageDelegate(pageView: PageView) : HorizontalPageDelegate(pageView) {

    override fun onAnimStart(animationSpeed: Int) {
        if (!isCancel) {
            pageView.fillPage(mDirection)
        }
        stopScroll()
    }

    override fun onDraw(canvas: Canvas) {
        // nothing
    }

    override fun onAnimStop() {
        // nothing
    }


}
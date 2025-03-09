package com.zx.read.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import com.zx.read.PreferKey
import com.zx.read.ReadBook
import com.zx.read.extensions.activity
import com.zx.read.extensions.getPrefBoolean
import com.zx.read.extensions.gone
import com.zx.read.extensions.invisible
import com.zx.read.extensions.visible
import com.zx.read.utils.AnimationUtilsSupport
import com.zx.readbook.R
import com.zx.readbook.databinding.ViewReadMenuBinding
import org.jetbrains.anko.sdk27.listeners.onClick

/**
 * 阅读界面菜单
 */
class ReadMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    var cnaShowMenu: Boolean = false
    private val callBack: CallBack get() = activity as CallBack
    private val binding = ViewReadMenuBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var menuTopIn: Animation
    private lateinit var menuTopOut: Animation
    private lateinit var menuBottomIn: Animation
    private lateinit var menuBottomOut: Animation

    private var onMenuOutEnd: (() -> Unit)? = null
    val showBrightnessView get() = context.getPrefBoolean(PreferKey.showBrightnessView, true)

    init {
        initAnimation()
        binding.vwBg.onClick { }
        bindEvent()
    }

    /**
     * 设置屏幕亮度
     */
    private fun setScreenBrightness(value: Int) {
        var brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (!brightnessAuto()) {
            brightness = value.toFloat()
            if (brightness < 1f) brightness = 1f
            brightness /= 255f
        }
        val params = activity?.window?.attributes
        params?.screenBrightness = brightness
        activity?.window?.attributes = params
    }

    fun runMenuIn() {
        this.visible()
        binding.titleBar.visible()
        binding.bottomMenu.visible()
        binding.titleBar.startAnimation(menuTopIn)
        binding.bottomMenu.startAnimation(menuBottomIn)
    }

    fun runMenuOut(onMenuOutEnd: (() -> Unit)? = null) {
        this.onMenuOutEnd = onMenuOutEnd
        if (this.isVisible) {
            binding.titleBar.startAnimation(menuTopOut)
            binding.bottomMenu.startAnimation(menuBottomOut)
        }
    }

    private fun brightnessAuto(): Boolean {
        return context.getPrefBoolean("brightnessAuto", true) || !showBrightnessView
    }

    private fun bindEvent() = with(binding){
        tvChapterName.onClick {

        }
        //阅读进度
        seekReadPage.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReadBook.skipToPage(seekBar.progress)
            }
        })

        //上一章
        tvPre.onClick { ReadBook.moveToPrevChapter(upContent = true, toLast = false) }

        //下一章
        tvNext.onClick { ReadBook.moveToNextChapter(true) }

        //目录
        llCatalog.onClick {
            runMenuOut {
                callBack.openChapterList()
            }
        }
        //界面
        llFont.onClick {
            runMenuOut {
                callBack.showAdjust()
            }
        }

        //设置
        llSetting.onClick {
            runMenuOut {
                callBack.showReadStyle()
            }
        }
    }

    private fun initAnimation() {
        //显示菜单
        menuTopIn = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_in)
        menuBottomIn = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_in)
        menuTopIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
            }

            override fun onAnimationEnd(animation: Animation) {
                binding.vwMenuBg.onClick { runMenuOut() }
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })

        //隐藏菜单
        menuTopOut = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_out)
        menuBottomOut =
            AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_out)
        menuTopOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.vwMenuBg.setOnClickListener(null)
            }

            override fun onAnimationEnd(animation: Animation) {
                this@ReadMenu.invisible()
                binding.titleBar.invisible()
                binding.bottomMenu.invisible()
                cnaShowMenu = false
                onMenuOutEnd?.invoke()
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })
    }


    fun upBookView() {
        ReadBook.curTextChapter?.let {
            binding.tvChapterName.text = it.title
            binding.tvChapterName.visible()
            binding.seekReadPage.max = it.pageSize.minus(1)
            binding.seekReadPage.progress = ReadBook.durPageIndex
            binding.tvPre.isEnabled = ReadBook.durChapterIndex != 0
            binding.tvNext.isEnabled = ReadBook.durChapterIndex != ReadBook.chapterSize - 1
        } ?: let {
            binding.tvChapterName.gone()
        }
    }

    fun setSeekPage(seek: Int) {
        binding.seekReadPage.progress = seek
    }

    interface CallBack {
        fun openChapterList()
        fun showAdjust()
        fun showReadStyle()
    }

}

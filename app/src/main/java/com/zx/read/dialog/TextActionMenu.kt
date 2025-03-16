package com.zx.read.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.view.isVisible
import com.zx.read.bean.TextChar
import com.zx.read.ui.LineType
import com.zx.readbook.R
import com.zx.readbook.databinding.PopupActionMenuBinding
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.toast
import java.util.*
import kotlin.collections.ArrayList

/**
 * 选择文字操作菜单
 */
class TextActionMenu(private val context: Context, private val callBack: CallBack) :
    PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    init {
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_action_menu, null)
        contentView.isClickable = true
        isTouchable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isOutsideTouchable = false
        isFocusable = true
        setOnDismissListener {

        }
        val actionMenuUnderline = contentView.findViewById<TextView>(R.id.action_menu_underline)
        val llActionMenuLayout = contentView.findViewById<LinearLayout>(R.id.ll_underline_style)
        val backgroundColor = contentView.findViewById<TextView>(R.id.tv_background_color)
        val wavyLine = contentView.findViewById<TextView>(R.id.tv_wavy_line)
        val dashedLine = contentView.findViewById<TextView>(R.id.tv_dashed_line)
        actionMenuUnderline?.setOnClickListener {
            if (llActionMenuLayout.isVisible) {
                llActionMenuLayout.visibility = View.GONE
            } else {
                llActionMenuLayout.visibility = View.VISIBLE
            }
        }
        backgroundColor.setOnClickListener {
            callBack.onUnderline(LineType.BACKGROUND_COLOR)
        }
        wavyLine.setOnClickListener {
            callBack.onUnderline(LineType.WAVY)
        }
        dashedLine.setOnClickListener {
            callBack.onUnderline(LineType.DASHED)
        }
    }


    interface CallBack {
        val selectedText: ArrayList<TextChar>
        fun onMenuActionFinally()
        fun onUnderline(type : LineType)

    }
}
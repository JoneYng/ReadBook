package com.zx.read.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.Menu
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.view.isVisible
import com.zx.readbook.R
import com.zx.readbook.databinding.PopupActionMenuBinding
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.toast
import java.util.*

@SuppressLint("RestrictedApi")
class TextActionMenu(private val context: Context, private val callBack: CallBack) :
    PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT){
    private val binding = PopupActionMenuBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_action_menu, null)
        isTouchable = true
        isOutsideTouchable = false
        isFocusable = false
        setOnDismissListener {

        }
    }






    interface CallBack {
        val selectedText: String

        fun onMenuActionFinally()
    }
}
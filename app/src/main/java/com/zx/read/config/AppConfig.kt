package com.zx.read.config

import android.annotation.SuppressLint
import android.content.Context
import com.zx.read.App
import com.zx.read.PreferKey
import com.zx.read.extensions.getPrefBoolean
import com.zx.read.extensions.getPrefInt
import com.zx.read.extensions.getPrefString
import com.zx.read.extensions.putPrefBoolean
import com.zx.read.extensions.putPrefInt
import com.zx.read.extensions.putPrefString
import com.zx.read.extensions.removePref
import com.zx.read.extensions.sysIsDarkMode


object AppConfig {

    var isEInkMode: Boolean = false

    fun isNightTheme(context: Context): Boolean {
        return when (context.getPrefString(PreferKey.themeMode, "0")) {
            "1" -> false
            "2" -> true
            "3" -> false
            else -> context.sysIsDarkMode()
        }
    }

    var isNightTheme: Boolean
        get() = isNightTheme(App.INSTANCE)
        set(value) {
            if (isNightTheme != value) {
                if (value) {
                    App.INSTANCE.putPrefString(PreferKey.themeMode, "2")
                } else {
                    App.INSTANCE.putPrefString(PreferKey.themeMode, "1")
                }
            }
        }

    fun upEInkMode() {
        isEInkMode = App.INSTANCE.getPrefString(PreferKey.themeMode) == "3"
    }

    var isTransparentStatusBar: Boolean
        get() = App.INSTANCE.getPrefBoolean(PreferKey.transparentStatusBar,true)
        set(value) {
            App.INSTANCE.putPrefBoolean(PreferKey.transparentStatusBar, value)
        }

//    val requestedDirection: String?
//        get() = App.INSTANCE.getPrefString(R.string.pk_requested_direction)

    var backupPath: String?
        get() = App.INSTANCE.getPrefString(PreferKey.backupPath)
        set(value) {
            if (value.isNullOrEmpty()) {
                App.INSTANCE.removePref(PreferKey.backupPath)
            } else {
                App.INSTANCE.putPrefString(PreferKey.backupPath, value)
            }
        }

    var threadCount: Int
        get() = App.INSTANCE.getPrefInt(PreferKey.threadCount, 16)
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.threadCount, value)
        }

    var importBookPath: String?
        get() = App.INSTANCE.getPrefString("importBookPath")
        set(value) {
            if (value == null) {
                App.INSTANCE.removePref("importBookPath")
            } else {
                App.INSTANCE.putPrefString("importBookPath", value)
            }
        }

    var ttsSpeechRate: Int
        get() = App.INSTANCE.getPrefInt(PreferKey.ttsSpeechRate, 5)
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.ttsSpeechRate, value)
        }

    val clickAllNext: Boolean get() = App.INSTANCE.getPrefBoolean(PreferKey.clickAllNext, false)

    var chineseConverterType: Int
        get() = App.INSTANCE.getPrefInt(PreferKey.chineseConverterType,1)
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.chineseConverterType, value)
        }

    var systemTypefaces: Int
        get() = App.INSTANCE.getPrefInt(PreferKey.systemTypefaces)
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.systemTypefaces, value)
        }

    var elevation: Int
        @SuppressLint("PrivateResource")
        get() = App.INSTANCE.getPrefInt(
            PreferKey.barElevation,
//            App.INSTANCE.resources.getDimension(R.dimen.design_appbar_elevation).toInt()
           0.toInt()
        )
        set(value) {
            App.INSTANCE.putPrefInt(PreferKey.barElevation, value)
        }

    var replaceEnableDefault: Boolean =
        App.INSTANCE.getPrefBoolean(PreferKey.replaceEnableDefault, true)


    val readBodyToLh: Boolean get() = App.INSTANCE.getPrefBoolean(PreferKey.readBodyToLh, true)

}

